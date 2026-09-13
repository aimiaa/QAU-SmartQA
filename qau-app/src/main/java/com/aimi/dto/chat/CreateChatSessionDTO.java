package com.aimi.dto.chat;

import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateChatSessionDTO(
        @Size(max = 255, message = "会话标题不能超过255个字符")
        String title,
        List<Long> knowledgeBaseIds
) {
}
