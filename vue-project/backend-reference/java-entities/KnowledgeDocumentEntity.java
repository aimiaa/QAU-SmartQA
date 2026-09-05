package com.qau.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 知识库文档：PDF、Word、通知附件等原始文件元数据。 */
@Data
@TableName("knowledge_document")
public class KnowledgeDocumentEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long knowledgeBaseId;
  private String title;
  private String fileName;
  private String fileType;
  private Long fileSize;
  private String storageUrl;
  private String checksum;
  private String status;
  private Integer versionNo;
  private Long uploadedBy;
  private LocalDateTime uploadedAt;
  private LocalDateTime parsedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  @TableLogic
  private Boolean deleted;
}
