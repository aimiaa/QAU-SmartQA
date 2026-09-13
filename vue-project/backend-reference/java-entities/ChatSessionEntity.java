package com.qau.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 对话会话，对应前端 ChatSession。 */
@Data
@TableName("chat_session")
public class ChatSessionEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long userId;
  private String title;
  private String scope;
  private Integer messageCount;
  private Boolean pinned;
  private LocalDateTime lastMessageAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  @TableLogic
  private Boolean deleted;
}
