package com.aimi.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiChatService {

    private final ObjectProvider<ChatClient> chatClientProvider;

    public AiChatService(ObjectProvider<ChatClient> chatClientProvider) {
        this.chatClientProvider = chatClientProvider;
    }

    /**
     * 同步调用 - 返回完整结果
     */
    public String chat(String userMessage) {
        ChatClient chatClient = chatClientProvider.getIfAvailable();
        if (chatClient == null) {
            return buildFallbackAnswer(userMessage);
        }

        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }

    /**
     * 流式调用 - 逐字返回（SSE）
     */
    public Flux<String> chatStream(String userMessage) {
        return getChatClient().prompt()
                .user(userMessage)
                .stream()
                .content();
    }

    /**
     * 带历史上下文的对话
     */
    public String chatWithHistory(String userMessage, List<Message> history) {
        List<Message> messages = new ArrayList<>(history);
        messages.add(new UserMessage(userMessage));

        return getChatClient().prompt()
                .messages(messages)
                .call()
                .content();
    }

    private ChatClient getChatClient() {
        ChatClient chatClient = chatClientProvider.getIfAvailable();
        if (chatClient == null) {
            throw new IllegalStateException("未配置可用的 AI 模型，请设置 DASHSCOPE_API_KEY");
        }
        return chatClient;
    }

    private String buildFallbackAnswer(String userMessage) {
        return "已收到你的问题：" + userMessage + "。当前后端 AI 模型尚未完成配置，联调阶段先返回这条兜底回复；配置 DASHSCOPE_API_KEY 后会自动切换为真实模型回答。";
    }
}
