package com.aimi.vo.chat;

public record ChatSessionVO(
        Long id,
        String sessionId,
        String title,
        String scope,
        Integer messageCount,
        String updatedAt,
        Boolean pinned
) {
}
