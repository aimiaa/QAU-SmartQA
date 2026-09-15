package com.aimi.service;

import com.aimi.dto.chat.ChatRequestDTO;
import com.aimi.dto.chat.CreateChatSessionDTO;
import com.aimi.service.impl.ChatConversationServiceImpl;
import com.aimi.vo.chat.ChatMessageVO;
import com.aimi.vo.chat.ChatReplyVO;
import com.aimi.vo.chat.ChatSessionVO;
import java.util.List;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * 对话应用服务：负责会话管理、消息持久化，以及调用 AI 完成问答与流式输出。
 * 具体实现见 {@link ChatConversationServiceImpl}。
 */
public interface ChatConversationService {

    /**
     * 非流式问答：保存用户提问，检索并生成回答，落库后返回完整回复。
     *
     * @param clientSessionId 前端传入的业务会话标识，可为空（为空时后端新建）
     * @param request         问答请求
     * @return 回复内容与最终会话标识
     */
    ChatReplyVO chat(String clientSessionId, ChatRequestDTO request);

    /**
     * 流式问答（SSE）：逐块推送 AI 回复，结束后再落库完整回答。
     *
     * @param clientSessionId 前端传入的业务会话标识，可为空
     * @param request         问答请求
     * @param userId          当前用户 ID（流式场景需在订阅前显式传入）
     * @return SSE 事件流
     */
    Flux<ServerSentEvent<String>> chatStream(String clientSessionId, ChatRequestDTO request, Long userId);

    /**
     * 创建新的对话会话。
     *
     * @param request 建会请求，可携带标题
     * @return 新建会话视图
     */
    ChatSessionVO createSession(CreateChatSessionDTO request);

    /**
     * 获取当前用户的会话列表，按置顶与更新时间倒序。
     *
     * @return 会话视图列表
     */
    List<ChatSessionVO> listSessions();

    /**
     * 获取指定会话下的消息明细（仅限会话归属人可见）。
     *
     * @param sessionId 业务会话标识
     * @return 消息视图列表；会话不存在或无权限时返回空列表
     */
    List<ChatMessageVO> listMessages(String sessionId);
}
