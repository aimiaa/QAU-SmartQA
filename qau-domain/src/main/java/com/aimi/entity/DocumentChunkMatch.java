package com.aimi.entity;

import lombok.Data;

/**
 * 向量相似度检索结果投影：切片正文 + 来源文档信息 + 相似度得分。
 * 非数据库表实体，仅用于 RAG 检索结果承载。
 */
@Data
public class DocumentChunkMatch {
    // 切片 ID
    private Long id;
    // 所属知识库 ID
    private Long knowledgeBaseId;
    // 所属文档 ID
    private Long documentId;
    // 切片正文
    private String content;
    // 来源文档标题（JOIN knowledge_document 得到）
    private String documentTitle;
    // 来源文件名
    private String fileName;
    // 余弦相似度，取值 [-1,1]，越接近 1 越相关
    private Double similarity;
}