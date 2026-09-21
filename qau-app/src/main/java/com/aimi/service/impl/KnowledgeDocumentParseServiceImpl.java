package com.aimi.service.impl;

import com.aimi.entity.DocumentChunkEntity;
import com.aimi.entity.KnowledgeBaseEntity;
import com.aimi.entity.KnowledgeDocumentEntity;
import com.aimi.exception.BusinessException;
import com.aimi.file.DocumentParseService;
import com.aimi.mapper.DocumentChunkMapper;
import com.aimi.mapper.KnowledgeBaseMapper;
import com.aimi.mapper.KnowledgeDocumentMapper;
import com.aimi.service.KnowledgeDocumentParseService;
import com.aimi.storage.FileStorageService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asynchronously parses uploaded documents, creates searchable chunks, and
 * updates document and knowledge-base states after each processing task.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentParseServiceImpl implements KnowledgeDocumentParseService {

    private static final String DOC_PARSING = "parsing";
    private static final String DOC_VECTORIZED = "vectorized";
    private static final String DOC_FAILED = "failed";
    private static final String KB_READY = "ready";
    private static final String KB_REVIEW = "review";

    /** Chunking defaults tuned for Chinese text and the configured embedding window. */
    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 100;

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final DocumentParseService documentParseService;
    private final FileStorageService fileStorageService;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;

    /**
     * 解析所需上下文全部通过参数传递：UserContext 是 ThreadLocal，
     * 在 kb-parse-* 线程里读不到，严禁在异步线程内取当前用户。
     */
    @Override
    @Async("knowledgeParseExecutor")
    @Transactional
    public void parseAsync(Long documentId) {
        // Hold the document row lock for the whole parse transaction so deletion waits
        // for parsing to finish and can remove all chunks without a stale async write.
        KnowledgeDocumentEntity document = knowledgeDocumentMapper.selectActiveForUpdate(documentId);
        if (document == null) {
            log.warn("Document not found when parsing, documentId={}", documentId);
            return;
        }
        Long kbId = document.getKnowledgeBaseId();
        try {
            updateDocument(documentId, DOC_PARSING, null);

            // 1. 取回原始文件并抽取文本
            byte[] raw = fileStorageService.retrieve(document.getStorageUrl());
            String text = documentParseService.extract(raw, document.getFileType());

            // 2. 切片
            List<String> chunks = split(text);
            if (chunks.isEmpty()) {
                throw new BusinessException("未能从文档中提取到有效文本");
            }

            // 3. 清理旧切片，保证同一文档重复解析幂等
            documentChunkMapper.deleteByDocumentId(documentId);

            // 4. 落切片；向量化失败只告警降级，文本切片照常保存，不阻断主流程
            saveChunks(kbId, documentId, chunks);

            updateDocument(documentId, DOC_VECTORIZED, LocalDateTime.now());
            log.info("Document parsed, documentId={}, kbId={}, chunks={}", documentId, kbId, chunks.size());
        } catch (Exception e) {
            log.error("Parse document failed, documentId={}, kbId={}", documentId, kbId, e);
            updateDocument(documentId, DOC_FAILED, null);
        } finally {
            // 状态收口：无论成败都聚合刷新，避免个别文档失败把知识库永久卡在 syncing
            refreshKnowledgeBaseStatus(kbId);
        }
    }

    /**
     * 聚合判定知识库状态。并发上传多个文档时，先完成的任务因 unfinished > 0 不会误改状态，
     * 最后完成的那个任务看到 unfinished = 0 将知识库推到 ready / review，天然无需加锁。
     */
    private void refreshKnowledgeBaseStatus(Long knowledgeBaseId) {
        try {
            if (knowledgeDocumentMapper.countUnfinished(knowledgeBaseId) > 0) {
                return;
            }
            long failed = knowledgeDocumentMapper.countFailed(knowledgeBaseId);

            KnowledgeBaseEntity update = new KnowledgeBaseEntity();
            update.setId(knowledgeBaseId);
            update.setStatus(failed > 0 ? KB_REVIEW : KB_READY);
            update.setLastSyncedAt(LocalDateTime.now());
            knowledgeBaseMapper.updateById(update);
        } catch (Exception e) {
            log.error("Refresh knowledge base status failed, kbId={}", knowledgeBaseId, e);
        }
    }

    private void saveChunks(Long kbId, Long documentId, List<String> chunks) {
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

        // Document parsing must still persist text chunks when embedding is disabled in tests or local setups.
        try {
            EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
            if (embeddingModel == null) {
                log.warn("Embedding model is not configured, keep plain chunks. documentId={}", documentId);
                documentChunkMapper.insertBatch(entities);
                return;
            }
            List<float[]> vectors = embeddingModel.embed(entities.stream().map(DocumentChunkEntity::getContent).toList());
            for (int i = 0; i < entities.size(); i++) {
                entities.get(i).setEmbedding(toVectorLiteral(vectors.get(i)));
            }
        } catch (Exception e) {
            log.warn("Embedding chunks failed, keep plain chunks. documentId={}", documentId, e);
        }

        documentChunkMapper.insertBatch(entities);
    }

    /** 定长滑窗切片，带重叠区避免语义在边界被切断。 */
    private List<String> split(String text) {
        String normalized = text == null ? "" : text.replace("\r\n", "\n").strip();
        List<String> result = new ArrayList<>();
        int step = CHUNK_SIZE - CHUNK_OVERLAP;
        for (int start = 0; start < normalized.length(); start += step) {
            int end = Math.min(start + CHUNK_SIZE, normalized.length());
            String piece = normalized.substring(start, end).strip();
            if (!piece.isEmpty()) {
                result.add(piece);
            }
            if (end == normalized.length()) {
                break;
            }
        }
        return result;
    }

    /** float[] 序列化为 pgvector 字面量，如 "[0.1,0.2,...]"，配合 XML 中的 ::vector 转换。 */
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

    private void updateDocument(Long documentId, String status, LocalDateTime parsedAt) {
        KnowledgeDocumentEntity update = new KnowledgeDocumentEntity();
        update.setId(documentId);
        update.setStatus(status);
        update.setParsedAt(parsedAt);
        knowledgeDocumentMapper.updateById(update);
    }
}
