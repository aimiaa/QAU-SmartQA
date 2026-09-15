package com.aimi.dto.knowledge;

/** 知识库列表查询条件，三个字段都可选，对应前端 KnowledgeBaseListParams。 */
public record KnowledgeBaseQueryDTO(
        // 关键词，模糊匹配名称、分类、描述
        String keyword,
        // 状态精确匹配：building / ready / syncing / review / failed
        String status,
        // 分类精确匹配，如教务、科研
        String category
) {
}