package com.aimi.dto.chat;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ChatRequestDTO(
        @NotBlank(message = "问题不能为空")
        String question,
        Long sessionId,
        List<Long> knowledgeBaseIds
) {
}
