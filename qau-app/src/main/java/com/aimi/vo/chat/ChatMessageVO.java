package com.aimi.vo.chat;

import java.util.List;

public record ChatMessageVO(
        Long id,
        String role,
        String content,
        String time,
        List<String> sources
) {
}
