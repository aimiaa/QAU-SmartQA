package com.aimi.service.impl;

import com.aimi.converter.KnowledgeDocumentConverter;
import com.aimi.entity.KnowledgeBaseEntity;
import com.aimi.entity.KnowledgeDocumentEntity;
import com.aimi.exception.BusinessException;
import com.aimi.exception.ErrorCode;
import com.aimi.mapper.KnowledgeBaseMapper;
import com.aimi.mapper.KnowledgeDocumentMapper;
import com.aimi.mapper.DocumentChunkMapper;
import com.aimi.mapper.KnowledgeSyncJobMapper;
import com.aimi.security.UserContext;
import com.aimi.service.KnowledgeDocumentService;
import com.aimi.storage.FileStorageService;
import com.aimi.vo.knowledge.KnowledgeDocumentVO;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档专属服务实现：承载文档上传的同步链路（校验 -> 存文件 -> 落库 -> 写入持久化解析任务），
 * 与知识库管理接口（KnowledgeService）解耦。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    /** 停用状态：与 KnowledgeServiceImpl 列表过滤的取值保持一致。 */
    private static final String STATUS_DISABLED = "disabled";

    /** 允许上传的文档扩展名。 */
    private static final Set<String> SUPPORTED_FILE_TYPES = Set.of("pdf", "docx", "doc", "txt", "md");
    private static final String DOC_STATUS_UPLOADED = "uploaded";

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final KnowledgeDocumentConverter knowledgeDocumentConverter;
    private final KnowledgeSyncJobMapper knowledgeSyncJobMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeDocumentVO> listDocuments(Long knowledgeBaseId) {
        requireKnowledgeBase(knowledgeBaseId);
        return knowledgeDocumentMapper.selectList(
                        new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                                .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                                .orderByDesc(KnowledgeDocumentEntity::getUploadedAt)
                                .orderByDesc(KnowledgeDocumentEntity::getId))
                .stream()
                .map(knowledgeDocumentConverter::toVO)
                .toList();
    }

    /**
     * 上传文档的同步部分：校验 -> 存文件 -> 落库 -> 写入 pending 任务。
     * 解析由持久化任务调度器负责，不能依赖进程内回调或线程池队列。
     */
    @Override
    @Transactional
    public KnowledgeDocumentVO uploadDocument(Long knowledgeBaseId, MultipartFile file) {
        // UserContext 基于 ThreadLocal，异步线程取不到，必须在请求线程内先取值再显式传递
        Long userId = UserContext.requireUserId();

        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        String fileName = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename().trim();
        String fileType = extensionOf(fileName);
        if (!SUPPORTED_FILE_TYPES.contains(fileType)) {
            throw new BusinessException("仅支持上传 PDF / Word / Markdown / 纯文本文档");
        }

        KnowledgeBaseEntity knowledgeBase = requireKnowledgeBase(knowledgeBaseId);
        if (STATUS_DISABLED.equals(knowledgeBase.getStatus())) {
            throw new BusinessException("知识库已停用，无法上传文档");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("读取上传文件失败：" + e.getMessage(), e);
        }

        String storageUrl = fileStorageService.store(content, fileType);
        registerStorageRollbackCleanup(storageUrl);

        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setTitle(stripExtension(fileName));
        document.setFileName(fileName);
        document.setFileType(fileType);
        document.setFileSize((long) content.length);
        document.setStorageUrl(storageUrl);
        document.setChecksum(sha256(content));
        document.setStatus(DOC_STATUS_UPLOADED);
        document.setVersionNo(1);
        document.setUploadedBy(userId);
        document.setDeleted(false);
        knowledgeDocumentMapper.insert(document);

        com.aimi.entity.KnowledgeSyncJobEntity job = new com.aimi.entity.KnowledgeSyncJobEntity();
        job.setKnowledgeBaseId(knowledgeBaseId);
        job.setDocumentId(document.getId());
        job.setJobType("parse");
        job.setStatus("pending");
        job.setProgress(0);
        job.setAttempts(0);
        job.setMaxAttempts(5);
        job.setNextRunAt(java.time.LocalDateTime.now());
        job.setCreatedBy(userId);
        knowledgeSyncJobMapper.insert(job);

        // 状态与计数在一条原子 UPDATE 里完成
        knowledgeBaseMapper.markSyncingAndIncreaseDocumentCount(knowledgeBaseId);

        log.info("Document uploaded, kbId={}, documentId={}, fileName={}, size={}",
                knowledgeBaseId, document.getId(), fileName, content.length);

        // 回查拿 DB 默认时间戳（uploaded_at 等），与 createKnowledgeBase 的回查模式一致
        return knowledgeDocumentConverter.toVO(knowledgeDocumentMapper.selectById(document.getId()));
    }

    @Override
    @Transactional
    public void deleteDocument(Long knowledgeBaseId, Long documentId) {
        requireKnowledgeBase(knowledgeBaseId);

        // Match the parser's row lock so deletion cannot race with chunk writes.
        KnowledgeDocumentEntity document = knowledgeDocumentMapper.selectActiveForUpdate(documentId);
        if (document != null && !knowledgeBaseId.equals(document.getKnowledgeBaseId())) {
            document = null;
        }
        if (document == null) {
            throw new BusinessException("文档不存在或不属于当前知识库");
        }

        // Remove searchable chunks before logically deleting the document metadata.
        documentChunkMapper.deleteByDocumentId(documentId);
        knowledgeSyncJobMapper.cancelActiveByDocumentId(documentId);
        if (knowledgeDocumentMapper.deleteById(documentId) > 0) {
            knowledgeBaseMapper.decreaseDocumentCount(knowledgeBaseId);
            refreshKnowledgeBaseStatus(knowledgeBaseId);
            String storageUrl = document.getStorageUrl();
            if (storageUrl != null && !storageUrl.isBlank()
                    && TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            fileStorageService.delete(storageUrl);
                        } catch (Exception e) {
                            // Database deletion is already committed; retain an actionable cleanup log.
                            log.error("Delete document storage object failed, documentId={}, storageUrl={}",
                                    documentId, storageUrl, e);
                        }
                    }
                });
            }
        }
    }

    private void registerStorageRollbackCleanup(String storageUrl) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        fileStorageService.delete(storageUrl);
                    } catch (Exception e) {
                        log.error("Delete orphaned document storage after rollback failed, storageUrl={}", storageUrl, e);
                    }
                }
            }
        });
    }

    private KnowledgeBaseEntity requireKnowledgeBase(Long knowledgeBaseId) {
        KnowledgeBaseEntity knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null || STATUS_DISABLED.equals(knowledgeBase.getStatus())) {
            throw new BusinessException(ErrorCode.KB_NOT_FOUND);
        }
        return knowledgeBase;
    }

    private void refreshKnowledgeBaseStatus(Long knowledgeBaseId) {
        if (knowledgeDocumentMapper.countActive(knowledgeBaseId) == 0) {
            KnowledgeBaseEntity update = new KnowledgeBaseEntity();
            update.setId(knowledgeBaseId);
            update.setStatus("building");
            update.setLastSyncedAt(java.time.LocalDateTime.now());
            knowledgeBaseMapper.updateById(update);
            return;
        }

        if (knowledgeDocumentMapper.countUnfinished(knowledgeBaseId) > 0) {
            return;
        }

        KnowledgeBaseEntity update = new KnowledgeBaseEntity();
        update.setId(knowledgeBaseId);
        update.setStatus(knowledgeDocumentMapper.countFailed(knowledgeBaseId) > 0 ? "review" : "ready");
        update.setLastSyncedAt(java.time.LocalDateTime.now());
        knowledgeBaseMapper.updateById(update);
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 || dot == fileName.length() - 1
                ? ""
                : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot <= 0 ? fileName : fileName.substring(0, dot);
    }

    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (Exception e) {
            // 校验码仅用于去重排查，生成失败不阻断上传
            log.warn("Compute document checksum failed", e);
            return null;
        }
    }
}
