package com.aimi.vo.knowledge;

/** 知识库列表项，字段名与前端 KnowledgeBase 一一对应。 */
public record KnowledgeBaseVO(
        // 主键 ID
        Long id,
        // 知识库名称
        String name,
        // 分类
        String category,
        // 状态：building / ready / syncing / review / failed
        String status,
        // 文档数量，前端字段名为 documents
        Integer documents,
        // 更新时间，前端直接展示，固定为 MM-dd HH:mm（如 09-15 14:06）
        String updatedAt,
        // 描述，前端会直接调用 toLowerCase，不允许为 null
        String description
) {
}