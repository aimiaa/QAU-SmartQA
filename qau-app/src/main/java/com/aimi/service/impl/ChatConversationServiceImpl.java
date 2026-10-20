package com.aimi.service.impl;

import com.aimi.chat.AiChatService;
import com.aimi.dto.chat.CreateChatSessionDTO;
import com.aimi.dto.chat.ChatRequestDTO;
import com.aimi.entity.ChatMessageEntity;
import com.aimi.entity.ChatMessageSourceEntity;
import com.aimi.entity.ChatSessionEntity;
import com.aimi.entity.DocumentChunkMatch;
import com.aimi.entity.KnowledgeBaseEntity;
import com.aimi.exception.BusinessException;
import com.aimi.exception.ErrorCode;
import com.aimi.mapper.ChatMessageMapper;
import com.aimi.mapper.ChatMessageSourceMapper;
import com.aimi.mapper.ChatSessionMapper;
import com.aimi.mapper.KnowledgeBaseMapper;
import com.aimi.rag.RagChatService;
import com.aimi.security.UserContext;
import com.aimi.service.ChatConversationService;
import com.aimi.vo.chat.ChatMessageVO;
import com.aimi.vo.chat.ChatReplyVO;
import com.aimi.vo.chat.ChatSessionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatConversationServiceImpl implements ChatConversationService {

    private static final String DEFAULT_SCOPE = "默认问答";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String ROLE_USER = "user";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int EXCERPT_MAX_LENGTH = 200;

    private final AiChatService aiChatService;
    private final RagChatService ragChatService;
    private final
    KnowledgeRetrievalService knowledgeRetrievalService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatMessageSourceMapper chatMessageSourceMapper;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    /** 检索前问题改写开关：把口语/追问补全为更适合检索的独立查询。 */
    @Value("${app.rag.query-rewrite.enabled:true}")
    private boolean queryRewriteEnabled;

    /** 问题改写时携带的历史消息条数（含用户与助手）。 */
    @Value("${app.rag.query-rewrite.history-messages:6}")
    private int historyMessages;

    /** 最终回答是否启用会话记忆。 */
    @Value("${app.rag.history.enabled:true}")
    private boolean historyEnabled;

    /** 最终回答最多携带的历史消息条数。 */
    @Value("${app.rag.history.max-messages:10}")
    private int maxHistoryMessages;

    /** 最终回答历史的字符预算，避免长回答挤占本轮问题和 RAG 上下文。 */
    @Value("${app.rag.history.max-chars:12000}")
    private int maxHistoryChars;

    public ChatConversationServiceImpl(
            AiChatService aiChatService,
            RagChatService ragChatService,
            KnowledgeRetrievalService knowledgeRetrievalService,
            KnowledgeBaseMapper knowledgeBaseMapper,
            ChatSessionMapper chatSessionMapper,
            ChatMessageMapper chatMessageMapper,
            ChatMessageSourceMapper chatMessageSourceMapper,
            TransactionTemplate transactionTemplate,
            ObjectMapper objectMapper
    ) {
        this.aiChatService = aiChatService;
        this.ragChatService = ragChatService;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.chatMessageSourceMapper = chatMessageSourceMapper;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ChatReplyVO chat(String clientSessionId, ChatRequestDTO request) {
        Long userId = UserContext.requireUserId();
        String sessionId = normalizeSessionId(clientSessionId);
        String question = request.question().strip();
        LocalDateTime now = LocalDateTime.now();

        ChatSessionEntity session = getOrCreateSession(sessionId, question, now, userId);
        List<Message> historyMessagesForAnswer = loadHistoryMessages(session.getId());
        String history = formatHistoryForRewrite(historyMessagesForAnswer);

        ChatMessageEntity userMessage = createMessage(session.getId(), null, ROLE_USER, question, now);
        chatMessageMapper.insert(userMessage);

        List<DocumentChunkMatch> matches =
                knowledgeRetrievalService.retrieve(effectiveQuery(question, history), request.knowledgeBaseIds());

        String answer;
        if (matches.isEmpty()) {
            answer = ragChatService.answerWithoutContext(
                    question, resolveKnowledgeBaseNames(request.knowledgeBaseIds()), historyMessagesForAnswer);
        } else {
            answer = ragChatService.answerWithContext(
                    question, knowledgeRetrievalService.buildContext(matches), historyMessagesForAnswer);
        }

        ChatMessageEntity assistantMessage = createMessage(session.getId(), userMessage.getId(), ROLE_ASSISTANT, answer, now);
        chatMessageMapper.insert(assistantMessage);
        saveSources(assistantMessage.getId(), matches);

        updateSession(session, question, now);
        return new ChatReplyVO(answer, sessionId, distinctSourceTitles(matches));
    }

    @Override
    public Flux<ServerSentEvent<String>> chatStream(String clientSessionId, ChatRequestDTO request, Long userId) {
        String sessionId = normalizeSessionId(clientSessionId);
        String question = request.question().strip();
        LocalDateTime now = LocalDateTime.now();

        ChatSessionEntity session = transactionTemplate.execute(status ->
                getOrCreateSession(sessionId, question, now, userId));
        List<Message> historyMessagesForAnswer = loadHistoryMessages(session.getId());
        String history = formatHistoryForRewrite(historyMessagesForAnswer);

        ChatMessageEntity userMessage = createMessage(session.getId(), null, ROLE_USER, question, now);
        transactionTemplate.executeWithoutResult(status ->
                chatMessageMapper.insert(userMessage));

        List<DocumentChunkMatch> matches =
                knowledgeRetrievalService.retrieve(effectiveQuery(question, history), request.knowledgeBaseIds());
        List<String> sourceTitles = distinctSourceTitles(matches);

        Flux<String> answerFlux;
        try {
            if (matches.isEmpty()) {
                answerFlux = ragChatService.answerWithoutContextStream(
                        question, resolveKnowledgeBaseNames(request.knowledgeBaseIds()), historyMessagesForAnswer);
            } else {
                answerFlux = ragChatService.answerWithContextStream(
                        question, knowledgeRetrievalService.buildContext(matches), historyMessagesForAnswer);
            }
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
                .concatWith(buildSourcesEvent(sourceTitles))
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
                    saveSources(assistantMessage.getId(), matches);
                    updateSession(session, question, completedAt);
                }));
    }

    @Override
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

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageVO> listMessages(String sessionId) {
        Long userId = UserContext.requireUserId();
        ChatSessionEntity session = findOwnedSession(sessionId, userId);
        if (session == null) {
            return List.of();
        }

        List<ChatMessageEntity> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessageEntity>()
                        .eq(ChatMessageEntity::getSessionId, session.getId())
                        .orderByAsc(ChatMessageEntity::getCreatedAt));

        List<Long> messageIds = messages.stream().map(ChatMessageEntity::getId).toList();
        Map<Long, List<String>> sourcesByMessage = loadSourcesByMessage(messageIds);

        return messages.stream()
                .map(message -> toMessageVO(message, sourcesByMessage.getOrDefault(message.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId) {
        Long userId = UserContext.requireUserId();
        ChatSessionEntity session = findOwnedSession(sessionId, userId);
        if (session == null) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        }

        // 逻辑删除会话下的全部消息（@TableLogic 生效，实际为 UPDATE deleted = true）
        chatMessageMapper.delete(
                new LambdaQueryWrapper<ChatMessageEntity>()
                        .eq(ChatMessageEntity::getSessionId, session.getId()));

        // 逻辑删除会话本身
        chatSessionMapper.deleteById(session.getId());

        log.info("Chat session deleted, sessionId={}, dbId={}, userId={}",
                sessionId, session.getId(), userId);
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

    private ChatMessageVO toMessageVO(ChatMessageEntity message, List<String> sources) {
        return new ChatMessageVO(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt() == null ? null : message.getCreatedAt().format(TIME_FORMATTER),
                sources == null ? List.of() : sources
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

    /** 检索前问题改写：失败或未开启时回退原始问题，保证检索链路稳健。 */
    private String effectiveQuery(String question, String history) {
        if (!queryRewriteEnabled) {
            return question;
        }
        try {
            String rewritten = ragChatService.rewriteQuery(question, history);
            return rewritten == null || rewritten.isBlank() ? question : rewritten.strip();
        } catch (Exception e) {
            log.warn("Query rewrite failed, fallback to raw question. question={}", question, e);
            return question;
        }
    }

    /** 加载本轮之前已完成的会话消息，并施加条数和字符预算。 */
    private List<Message> loadHistoryMessages(Long sessionId) {
        if (!historyEnabled || sessionId == null || maxHistoryMessages <= 0) {
            return List.of();
        }
        List<ChatMessageEntity> recent = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessageEntity>()
                        .eq(ChatMessageEntity::getSessionId, sessionId)
                        .orderByDesc(ChatMessageEntity::getCreatedAt)
                        .orderByDesc(ChatMessageEntity::getId)
                        .last("LIMIT " + maxHistoryMessages));
        if (recent.isEmpty()) {
            return List.of();
        }
        List<ChatMessageEntity> ordered = new ArrayList<>(recent);
        List<Message> result = new ArrayList<>();
        int usedChars = 0;
        for (ChatMessageEntity message : ordered) {
            if (message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }
            Message converted = toAiMessage(message);
            if (converted == null) {
                continue;
            }
            int messageChars = message.getContent().length();
            if (maxHistoryChars > 0 && !result.isEmpty() && usedChars + messageChars > maxHistoryChars) {
                continue;
            }
            result.add(0, converted);
            usedChars += messageChars;
        }
        return result;
    }

    /** 组装查询改写所需的历史；助手回答截断，避免改写请求过大。 */
    private String formatHistoryForRewrite(List<Message> history) {
        if (!queryRewriteEnabled || history == null || history.isEmpty() || historyMessages <= 0) {
            return "";
        }
        return history.stream()
                .limit(historyMessages)
                .map(message -> {
                    String content = message.getText();
                    if (message instanceof AssistantMessage && content.length() > EXCERPT_MAX_LENGTH) {
                        content = content.substring(0, EXCERPT_MAX_LENGTH) + "...";
                    }
                    return (message instanceof UserMessage ? "用户：" : "助手：") + content;
                })
                .collect(Collectors.joining("\n"));
    }

    private Message toAiMessage(ChatMessageEntity message) {
        if (ROLE_USER.equals(message.getRole())) {
            return new UserMessage(message.getContent());
        }
        if (ROLE_ASSISTANT.equals(message.getRole())) {
            return new AssistantMessage(message.getContent());
        }
        return null;
    }

    /** 把本轮命中的切片作为来源落库，供历史消息与前端来源标签展示。 */
    private void saveSources(Long messageId, List<DocumentChunkMatch> matches) {
        if (messageId == null || matches == null || matches.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (DocumentChunkMatch m : matches) {
            ChatMessageSourceEntity source = new ChatMessageSourceEntity();
            source.setMessageId(messageId);
            source.setKnowledgeBaseId(m.getKnowledgeBaseId());
            source.setDocumentId(m.getDocumentId());
            source.setChunkId(m.getId());
            source.setSourceTitle(m.getDocumentTitle() != null ? m.getDocumentTitle() : m.getFileName());
            source.setSourceExcerpt(truncate(m.getContent()));
            source.setSimilarityScore(toScore(m.getSimilarity()));
            source.setCreatedAt(now);
            chatMessageSourceMapper.insert(source);
        }
    }

    /** 批量加载多条消息的来源标题（去重保序），避免历史消息逐条查询造成 N+1。 */
    private Map<Long, List<String>> loadSourcesByMessage(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Map.of();
        }
        List<ChatMessageSourceEntity> rows = chatMessageSourceMapper.selectList(
                new LambdaQueryWrapper<ChatMessageSourceEntity>()
                        .in(ChatMessageSourceEntity::getMessageId, messageIds)
                        .orderByAsc(ChatMessageSourceEntity::getId));
        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (ChatMessageSourceEntity row : rows) {
            if (row.getSourceTitle() == null) {
                continue;
            }
            List<String> titles = result.computeIfAbsent(row.getMessageId(), k -> new ArrayList<>());
            if (!titles.contains(row.getSourceTitle())) {
                titles.add(row.getSourceTitle());
            }
        }
        return result;
    }

    /** 来源标题去重保序，供 SSE sources 事件与同步回复返回。 */
    private List<String> distinctSourceTitles(List<DocumentChunkMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return List.of();
        }
        return matches.stream()
                .map(m -> m.getDocumentTitle() != null ? m.getDocumentTitle() : m.getFileName())
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new), List::copyOf));
    }

    /** 构造 sources SSE 事件（JSON 数组字符串）；序列化失败则跳过，不影响正文与 done。 */
    private Flux<ServerSentEvent<String>> buildSourcesEvent(List<String> sourceTitles) {
        if (sourceTitles == null || sourceTitles.isEmpty()) {
            return Flux.empty();
        }
        try {
            String json = objectMapper.writeValueAsString(sourceTitles);
            return Flux.just(ServerSentEvent.<String>builder().event("sources").data(json).build());
        } catch (Exception e) {
            log.warn("Serialize sources event failed", e);
            return Flux.empty();
        }
    }

    private String truncate(String content) {
        if (content == null) {
            return null;
        }
        String stripped = content.strip();
        return stripped.length() <= EXCERPT_MAX_LENGTH
                ? stripped
                : stripped.substring(0, EXCERPT_MAX_LENGTH);
    }

    private BigDecimal toScore(Double similarity) {
        if (similarity == null) {
            return null;
        }
        return BigDecimal.valueOf(similarity).setScale(6, RoundingMode.HALF_UP);
    }

    /** 兜底模板需要展示已选择的知识库名称；未指定范围时提示为全部知识库。 */
    private String resolveKnowledgeBaseNames(List<Long> knowledgeBaseIds) {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return "全部知识库";
        }
        return knowledgeBaseMapper.selectList(
                        new LambdaQueryWrapper<KnowledgeBaseEntity>()
                                .in(KnowledgeBaseEntity::getId, knowledgeBaseIds))
                .stream()
                .map(KnowledgeBaseEntity::getName)
                .collect(Collectors.joining("、"));
    }
}
