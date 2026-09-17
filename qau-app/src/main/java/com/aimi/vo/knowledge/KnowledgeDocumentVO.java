package com.aimi.vo.knowledge;

/** 知识库文档，字段名与前端 KnowledgeDocument 一一对应。 */
public record KnowledgeDocumentVO(
        Long id,
        Long knowledgeBaseId,
        String title,
        String fileName,
        String fileType,
        Long fileSize,
        // 前端 KnowledgeDocumentStatus：uploaded / parsing / ready / failed（DB 的 vectorized 映射为 ready）
        String status,
        String uploadedAt,
        String parsedAt
) {
}
