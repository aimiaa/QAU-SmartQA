package com.aimi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 每日使用统计：用于数据看板和首页指标趋势。 */
@Data
@TableName("usage_stat_daily")
public class UsageStatDailyEntity {
    // 主键 ID
    @TableId(type = IdType.AUTO)
    private Long id;
    // 统计日期（每日一条，唯一）
    private LocalDate statDate;
    // 当日对话次数
    private Integer chatCount;
    // 当日搜索次数
    private Integer searchCount;
    // 当日活跃用户数
    private Integer activeUserCount;
    // 当日文档总数
    private Integer documentCount;
    // 当日知识库总数
    private Integer knowledgeBaseCount;
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间（由数据库触发器自动维护）
    private LocalDateTime updatedAt;
}
