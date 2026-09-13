package com.qau.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 会话与知识库关联：一次对话可选择多个知识库。 */
@Data
@TableName("chat_session_knowledge_base")
public class ChatSessionKnowledgeBaseEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long sessionId;
  private Long knowledgeBaseId;
  private LocalDateTime createdAt;
}
