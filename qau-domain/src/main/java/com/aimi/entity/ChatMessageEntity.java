package com.aimi.entity;

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
    // 主键 ID
    @TableId(type = IdType.AUTO)
    private Long id;
    // 所属会话 ID
    private Long sessionId;
    // 父消息 ID（用于追问/重新生成等分支场景）
    private Long parentMessageId;
    // 消息角色：user 用户 / assistant AI / system 系统
    private String role;
    // 消息内容
    private String content;
    // 生成该回复所使用的模型名称
    private String modelName;
    // 本次消息消耗的 token 数
    private Integer tokenCount;
    // 创建时间
    private LocalDateTime createdAt;
    // 逻辑删除标记
    @TableLogic
    private Boolean deleted;
}
