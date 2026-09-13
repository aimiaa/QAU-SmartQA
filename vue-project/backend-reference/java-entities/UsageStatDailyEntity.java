package com.qau.ai.domain.entity;

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
  @TableId(type = IdType.AUTO)
  private Long id;
  private LocalDate statDate;
  private Integer chatCount;
  private Integer searchCount;
  private Integer activeUserCount;
  private Integer documentCount;
  private Integer knowledgeBaseCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
