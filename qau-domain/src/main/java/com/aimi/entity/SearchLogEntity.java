package com.aimi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 全局搜索记录：用于搜索历史与看板统计。 */
@Data
@TableName("search_log")
public class SearchLogEntity {
    // 主键 ID
    @TableId(type = IdType.AUTO)
    private Long id;
    // 操作用户 ID
    private Long userId;
    // 搜索关键词
    private String keyword;
    // 本次搜索限定的知识库 ID 集合（PostgreSQL BIGINT[] 数组）
    private Long[] knowledgeBaseIds;
    // 命中结果数
    private Integer resultCount;
    // 创建时间
    private LocalDateTime createdAt;
}
