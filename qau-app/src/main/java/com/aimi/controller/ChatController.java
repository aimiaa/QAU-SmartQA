package com.aimi.controller;

import com.aimi.dto.chat.ChatRequestDTO;
import com.aimi.dto.chat.CreateChatSessionDTO;
import com.aimi.result.Result;
import com.aimi.security.UserContext;
import com.aimi.service.ChatConversationService;
import com.aimi.vo.chat.ChatMessageVO;
import com.aimi.vo.chat.ChatReplyVO;
import com.aimi.vo.chat.ChatSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/chat")
@Tag(name = "chat", description = "Chat related operations")
public class ChatController {

    private static final String SESSION_ID_HEADER = "X-Session-Id";

    @Autowired
    private ChatConversationService chatConversationService;

    /**
     * 创建聊天会话
     * @param request
     * @return
     */
    @Operation(summary = "创建聊天会话")
    @PostMapping(value = "/sessions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<ChatSessionVO> createSession(
            @Valid @RequestBody(required = false) CreateChatSessionDTO request
    ) {
        return Result.success(chatConversationService.createSession(request));
    }

    /**
     * 获取聊天会话历史
     * @return
     */
    @Operation(summary = "获取聊天会话历史")
    @GetMapping("/sessions")
    public Result<List<ChatSessionVO>> listSessions() {
        return Result.success(chatConversationService.listSessions());
    }

    /**
     * 获取会话消息
     * @param sessionId
     * @return
     */
    @Operation(summary = "获取会话消息")
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ChatMessageVO>> listMessages(@PathVariable String sessionId) {
        return Result.success(chatConversationService.listMessages(sessionId));
    }

    /**
     * ai聊天系统
     * @param request
     * @return
     */
    @Operation(summary = "AI聊天系统")
    @PostMapping(value = "/message", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<ChatReplyVO> chat(
            @RequestHeader(value = SESSION_ID_HEADER, required = false) String sessionId,
            @Valid @RequestBody ChatRequestDTO request
    ) {
        log.info("Received chat question, sessionId={}, length={}", sessionId, request.question().length());
        return Result.success(chatConversationService.chat(sessionId, request));
    }

    /**
     * AI 聊天系统 - 流式输出（SSE）
     */
    @Operation(summary = "AI聊天系统-流式输出")
    @PostMapping(value = "/message/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(
            @RequestHeader(value = SESSION_ID_HEADER, required = false) String sessionId,
            @Valid @RequestBody ChatRequestDTO request
    ) {
        Long userId = UserContext.requireUserId();
        log.info("Received stream chat request, sessionId={}, length={}", sessionId, request.question().length());
        return chatConversationService.chatStream(sessionId, request, userId);
    }

}
