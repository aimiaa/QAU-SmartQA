package com.aimi.entity;

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
    // 主键 ID
    @TableId(type = IdType.AUTO)
    private Long id;
    // 业务会话 ID（对外暴露的唯一标识，用于前后端交互）
    private String sessionId;
    // 所属用户 ID
    private Long userId;
    // 会话标题
    private String title;
    // 会话主要知识库范围说明，用于前端展示
    private String scope;
    // 消息总数
    private Integer messageCount;
    // 是否置顶
    private Boolean pinned;
    // 最后一条消息时间
    private LocalDateTime lastMessageAt;
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间（由数据库触发器自动维护）
    private LocalDateTime updatedAt;
    // 逻辑删除标记
    @TableLogic
    private Boolean deleted;
}
