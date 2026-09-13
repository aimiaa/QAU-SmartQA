package com.qau.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 聊天消息，对应前端 ChatMessage。 */
@Data
@TableName("chat_message")
public class ChatMessageEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long sessionId;
  private Long parentMessageId;
  private String role;
  private String content;
  private String modelName;
  private Integer tokenCount;
  private LocalDateTime createdAt;
  @TableLogic
  private Boolean deleted;
}
