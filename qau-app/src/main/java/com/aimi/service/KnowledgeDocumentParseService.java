package com.aimi.service;

/**
 * 文档解析专属服务：负责 uploaded -> parsing -> vectorized / failed 的异步解析链路，
 * 与知识库管理接口（KnowledgeService）解耦。
 */
public interface KnowledgeDocumentParseService {

    /**
     * 异步解析指定文档：取回原文、抽取文本、切片、向量化落库，
     * 并在任务结束时聚合刷新所属知识库状态。
     *
     * @param documentId 待解析文档 ID
     */
    void parseAsync(Long documentId);
}
