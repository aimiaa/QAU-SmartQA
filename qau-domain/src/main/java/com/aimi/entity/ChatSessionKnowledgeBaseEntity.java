package com.aimi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 会话与知识库关联：一次对话可选择多个知识库。 */
@Data
@TableName("chat_session_knowledge_base")
public class ChatSessionKnowledgeBaseEntity {
    // 主键 ID
    @TableId(type = IdType.AUTO)
    private Long id;
    // 会话 ID
    private Long sessionId;
    // 知识库 ID
    private Long knowledgeBaseId;
    // 创建时间
    private LocalDateTime createdAt;
}
