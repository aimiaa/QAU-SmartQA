package com.aimi.dto.knowledge;

public record KnowledgeBaseDTO(
        // 知识库名称，必填
        String name,
        // 分类，为空时兜底为“未分类”
        String category,
        // 描述，可为空
        String description
) {
}
