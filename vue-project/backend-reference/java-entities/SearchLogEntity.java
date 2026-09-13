package com.qau.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 全局搜索记录：用于搜索历史与看板统计。 */
@Data
@TableName("search_log")
public class SearchLogEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long userId;
  private String keyword;
  private Long[] knowledgeBaseIds;
  private Integer resultCount;
  private LocalDateTime createdAt;
}
