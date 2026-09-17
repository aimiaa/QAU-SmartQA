package com.aimi.service;

import com.aimi.vo.knowledge.KnowledgeDocumentVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档专属服务：文档上传等同步操作收口在这里，
 * KnowledgeService 只保留知识库本身的管理接口。
 */
public interface KnowledgeDocumentService {

    /**
     * 向指定知识库上传文档：同步保存文件并落库，随后异步触发解析与向量化。
     *
     * @param knowledgeBaseId 目标知识库 ID
     * @param file            上传的文件（pdf/docx/doc/txt/md）
     * @return 文档 VO（status=uploaded），前端凭此轮询状态
     */
    KnowledgeDocumentVO uploadDocument(Long knowledgeBaseId, MultipartFile file);
}
