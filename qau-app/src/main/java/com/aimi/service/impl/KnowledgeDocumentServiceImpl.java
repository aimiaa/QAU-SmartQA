package com.aimi.service.impl;

import com.aimi.converter.KnowledgeDocumentConverter;
import com.aimi.entity.KnowledgeBaseEntity;
import com.aimi.entity.KnowledgeDocumentEntity;
import com.aimi.exception.BusinessException;
import com.aimi.exception.ErrorCode;
import com.aimi.mapper.KnowledgeBaseMapper;
import com.aimi.mapper.KnowledgeDocumentMapper;
import com.aimi.security.UserContext;
import com.aimi.service.KnowledgeDocumentParseService;
import com.aimi.service.KnowledgeDocumentService;
import com.aimi.storage.FileStorageService;
import com.aimi.vo.knowledge.KnowledgeDocumentVO;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档专属服务实现：承载文档上传的同步链路（校验 -> 存文件 -> 落库 -> 派发异步解析），
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
    private final KnowledgeDocumentConverter knowledgeDocumentConverter;
    private final KnowledgeDocumentParseService knowledgeDocumentParseService;
    private final FileStorageService fileStorageService;

    /**
     * 上传文档的同步部分：校验 -> 存文件 -> 落库 -> 知识库置 syncing 并原子加文档数 -> 事务提交后派发异步解析。
     * 这里刻意不同步做解析：PDF/Word 解析加向量化耗时不可控，不能占住 HTTP 线程。
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

        KnowledgeBaseEntity knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new BusinessException(ErrorCode.KB_NOT_FOUND);
        }
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

        // 状态与计数在一条原子 UPDATE 里完成
        knowledgeBaseMapper.markSyncingAndIncreaseDocumentCount(knowledgeBaseId);

        Long documentId = document.getId();
        // 事务提交后再派发：异步线程是独立连接，读不到未提交的数据
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                knowledgeDocumentParseService.parseAsync(documentId);
            }
        });

        log.info("Document uploaded, kbId={}, documentId={}, fileName={}, size={}",
                knowledgeBaseId, documentId, fileName, content.length);

        // 回查拿 DB 默认时间戳（uploaded_at 等），与 createKnowledgeBase 的回查模式一致
        return knowledgeDocumentConverter.toVO(knowledgeDocumentMapper.selectById(documentId));
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
