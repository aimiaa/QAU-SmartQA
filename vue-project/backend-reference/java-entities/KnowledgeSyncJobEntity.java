package com.qau.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 知识库同步任务：解析文档、生成向量、全量同步。 */
@Data
@TableName("knowledge_sync_job")
public class KnowledgeSyncJobEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long knowledgeBaseId;
  private Long documentId;
  private String jobType;
  private String status;
  private Integer progress;
  private String errorMessage;
  private LocalDateTime startedAt;
  private LocalDateTime finishedAt;
  private Long createdBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
