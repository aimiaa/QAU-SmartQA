package com.aimi.vo.chat;

import java.util.List;

public record ChatReplyVO(String answer, String sessionId, List<String> sources) {
}
