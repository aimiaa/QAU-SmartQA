package com.aimi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** AI 回复来源：记录回答引用的知识库、文档和文本切片。 */
@Data
@TableName("chat_message_source")
public class ChatMessageSourceEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long messageId;
    private Long knowledgeBaseId;
    private Long documentId;
    private Long chunkId;
    private String sourceTitle;
    private String sourceExcerpt;
    private BigDecimal similarityScore;
    private LocalDateTime createdAt;
}
