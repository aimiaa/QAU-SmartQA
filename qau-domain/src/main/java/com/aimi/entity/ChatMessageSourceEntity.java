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
    // 主键 ID
    @TableId(type = IdType.AUTO)
    private Long id;
    // 关联的消息 ID
    private Long messageId;
    // 引用的知识库 ID
    private Long knowledgeBaseId;
    // 引用的文档 ID
    private Long documentId;
    // 引用的文本切片 ID
    private Long chunkId;
    // 来源标题
    private String sourceTitle;
    // 来源摘录片段
    private String sourceExcerpt;
    // 相似度得分（向量检索命中度）
    private BigDecimal similarityScore;
    // 创建时间
    private LocalDateTime createdAt;
}
