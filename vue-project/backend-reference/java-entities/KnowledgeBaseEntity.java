package com.qau.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 知识库，对应前端 KnowledgeBase。 */
@Data
@TableName("knowledge_base")
public class KnowledgeBaseEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private String name;
  private String category;
  private String status;
  private String description;
  private Integer documentCount;
  private String ownerDepartment;
  private LocalDateTime lastSyncedAt;
  private Long createdBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  @TableLogic
  private Boolean deleted;
}
