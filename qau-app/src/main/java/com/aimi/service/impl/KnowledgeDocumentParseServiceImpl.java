package com.aimi.service.impl;

import com.aimi.entity.DocumentChunkEntity;
import com.aimi.entity.KnowledgeBaseEntity;
import com.aimi.entity.KnowledgeDocumentEntity;
import com.aimi.entity.KnowledgeSyncJobEntity;
import com.aimi.exception.BusinessException;
import com.aimi.file.DocumentParseService;
import com.aimi.mapper.DocumentChunkMapper;
import com.aimi.mapper.KnowledgeBaseMapper;
import com.aimi.mapper.KnowledgeDocumentMapper;
import com.aimi.mapper.KnowledgeSyncJobMapper;
import com.aimi.rag.TextDocumentChunker;
import com.aimi.service.KnowledgeDocumentParseService;
import com.aimi.storage.FileStorageService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Executes one already-claimed persistent parsing job. */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentParseServiceImpl implements KnowledgeDocumentParseService {

    private static final String DOC_UPLOADED = "uploaded";
    private static final String DOC_PARSING = "parsing";
    private static final String DOC_VECTORIZED = "vectorized";
    private static final String DOC_FAILED = "failed";
    private static final String KB_READY = "ready";
    private static final String KB_REVIEW = "review";

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final KnowledgeSyncJobMapper knowledgeSyncJobMapper;
    private final DocumentParseService documentParseService;
    private final FileStorageService fileStorageService;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final TextDocumentChunker textDocumentChunker;
    private final PlatformTransactionManager transactionManager;

    /** The dispatcher owns the thread pool; this method itself is deliberately synchronous. */
    @Override
    public void parse(KnowledgeSyncJobEntity job) {
        if (job == null || job.getDocumentId() == null || job.getWorkerId() == null) {
            log.warn("Ignore malformed knowledge sync job: {}", job);
            return;
        }
        Long documentId = job.getDocumentId();
        KnowledgeDocumentEntity document = knowledgeDocumentMapper.selectById(documentId);
        if (document == null || Boolean.TRUE.equals(document.getDeleted())) {
            finishFailure(job, new BusinessException("文档不存在或已删除"), true);
            return;
        }

        Long kbId = document.getKnowledgeBaseId();
        try {
            knowledgeDocumentMapper.updateProcessingStatus(documentId, DOC_PARSING, null);
            byte[] raw = fileStorageService.retrieve(document.getStorageUrl());
            String text = documentParseService.extract(raw, document.getFileType());
            List<String> chunks = textDocumentChunker.chunk(text);
            if (chunks.isEmpty()) {
                throw new BusinessException("未能从文档中提取到有效文本");
            }
            List<DocumentChunkEntity> entities = buildChunks(kbId, documentId, chunks);

            // Only database writes are inside the transaction. File IO and model calls above
            // must not hold a database connection or row lock for the duration of parsing.
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                KnowledgeDocumentEntity active = knowledgeDocumentMapper.selectActiveForUpdate(documentId);
                if (active == null) {
                    throw new BusinessException("文档已删除");
                }
                if (knowledgeSyncJobMapper.hasValidLease(job.getId(), job.getWorkerId()) == 0) {
                    throw new IllegalStateException("任务租约已失效");
                }
                documentChunkMapper.deleteByDocumentId(documentId);
                documentChunkMapper.insertBatch(entities);
                knowledgeDocumentMapper.updateProcessingStatus(documentId, DOC_VECTORIZED, LocalDateTime.now());
                if (knowledgeSyncJobMapper.markSuccess(job.getId(), job.getWorkerId()) == 0) {
                    throw new IllegalStateException("任务租约已失效，未确认成功");
                }
            });
            log.info("Document parsed, documentId={}, kbId={}, chunks={}", documentId, kbId, entities.size());
        } catch (Exception e) {
            log.error("Parse document failed, documentId={}, kbId={}", documentId, kbId, e);
            finishFailure(job, e, isPermanent(e));
        } finally {
            refreshKnowledgeBaseStatus(kbId);
        }
    }

    private List<DocumentChunkEntity> buildChunks(Long kbId, Long documentId, List<String> chunks) {
        List<DocumentChunkEntity> entities = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunkEntity chunk = new DocumentChunkEntity();
            chunk.setKnowledgeBaseId(kbId);
            chunk.setDocumentId(documentId);
            chunk.setChunkIndex(i);
            chunk.setContent(chunks.get(i));
            chunk.setTokenCount(chunks.get(i).length());
            entities.add(chunk);
        }
        try {
            EmbeddingModel model = embeddingModelProvider.getIfAvailable();
            if (model != null) {
                List<float[]> vectors = model.embed(entities.stream().map(DocumentChunkEntity::getContent).toList());
                for (int i = 0; i < entities.size(); i++) {
                    entities.get(i).setEmbedding(toVectorLiteral(vectors.get(i)));
                }
            } else {
                log.warn("Embedding model is not configured, keep plain chunks. documentId={}", documentId);
            }
        } catch (Exception e) {
            log.warn("Embedding chunks failed, keep plain chunks. documentId={}", documentId, e);
        }
        return entities;
    }

    private void finishFailure(KnowledgeSyncJobEntity job, Exception error, boolean permanent) {
        int attempts = job.getAttempts() == null ? 1 : job.getAttempts();
        int maxAttempts = job.getMaxAttempts() == null ? 5 : job.getMaxAttempts();
        boolean terminal = permanent || attempts >= maxAttempts;
        String status = terminal ? "dead" : "failed";
        LocalDateTime nextRun = terminal ? null : LocalDateTime.now().plusMinutes(backoffMinutes(attempts));
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        int updated = knowledgeSyncJobMapper.markFailure(job.getId(), job.getWorkerId(), status, terminal, nextRun,
                error.getClass().getSimpleName(), message);
        if (updated == 0) {
            return;
        }
        if (terminal && job.getDocumentId() != null) {
            knowledgeDocumentMapper.updateProcessingStatus(job.getDocumentId(), DOC_FAILED, null);
        } else if (!terminal && job.getDocumentId() != null) {
            knowledgeDocumentMapper.updateProcessingStatus(job.getDocumentId(), DOC_UPLOADED, null);
        }
    }

    private boolean isPermanent(Exception error) {
        return error instanceof BusinessException
                || error instanceof IllegalArgumentException
                || error instanceof UnsupportedOperationException;
    }

    private long backoffMinutes(int attempts) {
        return switch (Math.max(1, Math.min(attempts, 5))) {
            case 1 -> 1;
            case 2 -> 5;
            case 3 -> 15;
            case 4 -> 30;
            default -> 60;
        };
    }

    private void refreshKnowledgeBaseStatus(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return;
        }
        try {
            if (knowledgeDocumentMapper.countUnfinished(knowledgeBaseId) > 0) {
                return;
            }
            KnowledgeBaseEntity update = new KnowledgeBaseEntity();
            update.setId(knowledgeBaseId);
            update.setStatus(knowledgeDocumentMapper.countFailed(knowledgeBaseId) > 0 ? KB_REVIEW : KB_READY);
            update.setLastSyncedAt(LocalDateTime.now());
            knowledgeBaseMapper.updateById(update);
        } catch (Exception e) {
            log.error("Refresh knowledge base status failed, kbId={}", knowledgeBaseId, e);
        }
    }

    private String toVectorLiteral(float[] vector) {
        if (vector == null || vector.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder(vector.length * 8).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
