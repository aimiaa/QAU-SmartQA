package com.qau.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 快捷问题，对应前端 QuickQuestion。 */
@Data
@TableName("quick_question")
public class QuickQuestionEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private String label;
  private String question;
  private String category;
  private Integer sortOrder;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  @TableLogic
  private Boolean deleted;
}
