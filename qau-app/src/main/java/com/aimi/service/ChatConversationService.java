package com.aimi.service;

import com.aimi.chat.AiChatService;
import com.aimi.dto.chat.CreateChatSessionDTO;
import com.aimi.dto.chat.ChatRequestDTO;
import com.aimi.entity.ChatMessageEntity;
import com.aimi.entity.ChatSessionEntity;
import com.aimi.exception.BusinessException;
import com.aimi.exception.ErrorCode;
import com.aimi.mapper.ChatMessageMapper;
import com.aimi.mapper.ChatSessionMapper;
import com.aimi.security.UserContext;
import com.aimi.vo.chat.ChatMessageVO;
import com.aimi.vo.chat.ChatReplyVO;
import com.aimi.vo.chat.ChatSessionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ChatConversationService {

    private static final String DEFAULT_SCOPE = "默认问答";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String ROLE_USER = "user";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final AiChatService aiChatService;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final TransactionTemplate transactionTemplate;

    public ChatConversationService(
            AiChatService aiChatService,
            ChatSessionMapper chatSessionMapper,
            ChatMessageMapper chatMessageMapper,
            TransactionTemplate transactionTemplate
    ) {
        this.aiChatService = aiChatService;
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional
    public ChatReplyVO chat(String clientSessionId, ChatRequestDTO request) {
        Long userId = UserContext.requireUserId();
        String sessionId = normalizeSessionId(clientSessionId);
        String question = request.question().strip();
        LocalDateTime now = LocalDateTime.now();

        ChatSessionEntity session = getOrCreateSession(sessionId, question, now, userId);
        ChatMessageEntity userMessage = createMessage(session.getId(), null, ROLE_USER, question, now);
        chatMessageMapper.insert(userMessage);

        String answer = aiChatService.chat(question);
        ChatMessageEntity assistantMessage = createMessage(session.getId(), userMessage.getId(), ROLE_ASSISTANT, answer, now);
        chatMessageMapper.insert(assistantMessage);

        updateSession(session, question, now);
        return new ChatReplyVO(answer, sessionId);
    }

    public Flux<ServerSentEvent<String>> chatStream(String clientSessionId, ChatRequestDTO request, Long userId) {
        String sessionId = normalizeSessionId(clientSessionId);
        String question = request.question().strip();
        LocalDateTime now = LocalDateTime.now();

        ChatSessionEntity session = transactionTemplate.execute(status ->
                getOrCreateSession(sessionId, question, now, userId));
        ChatMessageEntity userMessage = createMessage(session.getId(), null, ROLE_USER, question, now);
        transactionTemplate.executeWithoutResult(status ->
                chatMessageMapper.insert(userMessage));

        Flux<String> answerFlux;
        try {
            answerFlux = aiChatService.chatStream(question);
        } catch (IllegalStateException e) {
            answerFlux = Flux.just("已收到你的问题：" + question + "。当前后端 AI 模型尚未完成配置，联调阶段先返回这条兜底回复。");
        }

        AtomicReference<StringBuilder> fullAnswer = new AtomicReference<>(new StringBuilder());

        return answerFlux
                .map(chunk -> {
                    fullAnswer.get().append(chunk);
                    return ServerSentEvent.<String>builder()
                            .event("message")
                            .data(chunk)
                            .build();
                })
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("[DONE]")
                                .build()
                ))
                .doOnComplete(() -> transactionTemplate.executeWithoutResult(status -> {
                    String answer = fullAnswer.get().toString();
                    LocalDateTime completedAt = LocalDateTime.now();
                    ChatMessageEntity assistantMessage = createMessage(
                            session.getId(), userMessage.getId(), ROLE_ASSISTANT, answer, completedAt);
                    chatMessageMapper.insert(assistantMessage);
                    updateSession(session, question, completedAt);
                }));
    }

    @Transactional
    public ChatSessionVO createSession(CreateChatSessionDTO request) {
        Long userId = UserContext.requireUserId();
        LocalDateTime now = LocalDateTime.now();
        String sessionId = UUID.randomUUID().toString();
        String title = request == null || request.title() == null || request.title().isBlank()
                ? "新对话"
                : request.title().strip();

        ChatSessionEntity session = new ChatSessionEntity();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setTitle(title);
        session.setScope(DEFAULT_SCOPE);
        session.setMessageCount(0);
        session.setPinned(false);
        session.setLastMessageAt(null);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        session.setDeleted(false);
        chatSessionMapper.insert(session);

        return toSessionVO(session);
    }

    @Transactional(readOnly = true)
    public List<ChatSessionVO> listSessions() {
        Long userId = UserContext.requireUserId();
        return chatSessionMapper.selectList(
                        new LambdaQueryWrapper<ChatSessionEntity>()
                                .eq(ChatSessionEntity::getUserId, userId)
                                .orderByDesc(ChatSessionEntity::getPinned)
                                .orderByDesc(ChatSessionEntity::getUpdatedAt)
                )
                .stream()
                .map(this::toSessionVO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageVO> listMessages(String sessionId) {
        Long userId = UserContext.requireUserId();
        ChatSessionEntity session = findOwnedSession(sessionId, userId);
        if (session == null) {
            return List.of();
        }

        return chatMessageMapper.selectList(
                        new LambdaQueryWrapper<ChatMessageEntity>()
                                .eq(ChatMessageEntity::getSessionId, session.getId())
                                .orderByAsc(ChatMessageEntity::getCreatedAt)
                )
                .stream()
                .map(this::toMessageVO)
                .toList();
    }

    private ChatSessionEntity getOrCreateSession(String sessionId, String question, LocalDateTime now, Long userId) {
        ChatSessionEntity existingSession = findSession(sessionId);
        if (existingSession != null) {
            if (!userId.equals(existingSession.getUserId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return existingSession;
        }

        ChatSessionEntity session = new ChatSessionEntity();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setTitle(buildTitle(question));
        session.setScope(DEFAULT_SCOPE);
        session.setMessageCount(0);
        session.setPinned(false);
        session.setLastMessageAt(now);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        session.setDeleted(false);

        chatSessionMapper.insert(session);
        return session;
    }

    private ChatSessionEntity findSession(String sessionId) {
        return chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSessionEntity>()
                        .eq(ChatSessionEntity::getSessionId, sessionId)
                        .last("LIMIT 1")
        );
    }

    /**
     * 按业务 sessionId 查找会话，并校验其归属于当前用户；命中他人会话时抛 403。
     */
    private ChatSessionEntity findOwnedSession(String sessionId, Long userId) {
        ChatSessionEntity session = findSession(sessionId);
        if (session == null) {
            return null;
        }
        if (!userId.equals(session.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return session;
    }

    private ChatSessionVO toSessionVO(ChatSessionEntity session) {
        return new ChatSessionVO(
                session.getId(),
                session.getSessionId(),
                session.getTitle(),
                session.getScope(),
                session.getMessageCount(),
                session.getUpdatedAt() == null ? null : session.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                session.getPinned()
        );
    }

    private ChatMessageVO toMessageVO(ChatMessageEntity message) {
        return new ChatMessageVO(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt() == null ? null : message.getCreatedAt().format(TIME_FORMATTER),
                List.of()
        );
    }

    private ChatMessageEntity createMessage(Long sessionId, Long parentMessageId, String role, String content, LocalDateTime now) {
        ChatMessageEntity message = new ChatMessageEntity();
        message.setSessionId(sessionId);
        message.setParentMessageId(parentMessageId);
        message.setRole(role);
        message.setContent(content);
        message.setTokenCount(0);
        message.setCreatedAt(now);
        message.setDeleted(false);
        return message;
    }

    private void updateSession(ChatSessionEntity session, String question, LocalDateTime now) {
        Integer currentCount = session.getMessageCount() == null ? 0 : session.getMessageCount();
        session.setMessageCount(currentCount + 2);
        session.setLastMessageAt(now);
        session.setUpdatedAt(now);

        if (session.getTitle() == null || session.getTitle().isBlank() || "新对话".equals(session.getTitle())) {
            session.setTitle(buildTitle(question));
        }

        chatSessionMapper.updateById(session);
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return UUID.randomUUID().toString();
        }

        return sessionId.strip();
    }

    private String buildTitle(String question) {
        if (question.length() <= 24) {
            return question;
        }

        return question.substring(0, 24) + "...";
    }
}
