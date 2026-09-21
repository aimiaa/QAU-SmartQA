package com.aimi.rag;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * RAG 生成服务：按场景加载 prompts/*.st 模板渲染 System/User 提示词，再交给 ChatClient 生成。
 * 命中上下文与未命中分别走不同模板，未命中时严格兜底不编造。
 */
@Service
public class RagChatService {

    private static final String SYSTEM_WITH_CONTEXT = "prompts/campus-qa-system.st";
    private static final String USER_WITH_CONTEXT = "prompts/campus-qa-user.st";
    private static final String SYSTEM_NO_CONTEXT = "prompts/campus-qa-no-context-system.st";
    private static final String USER_NO_CONTEXT = "prompts/campus-qa-no-context-user.st";
    private static final String QUERY_REWRITE = "prompts/campus-qa-query-rewrite.st";

    private final ObjectProvider<ChatClient> chatClientProvider;

    public RagChatService(ObjectProvider<ChatClient> chatClientProvider) {
        this.chatClientProvider = chatClientProvider;
    }

    /** 检索前问题改写：把口语/追问补全为适合向量检索的独立查询（可选增强）。 */
    public String rewriteQuery(String question, String history) {
        String userPrompt = render(QUERY_REWRITE, Map.of(
                "history", history == null ? "" : history,
                "question", question));
        return getChatClient().prompt().user(userPrompt).call().content().strip();
    }

    /** 命中知识库片段时的同步作答。 */
    public String answerWithContext(String question, String context) {
        return getChatClient().prompt()
                .system(render(SYSTEM_WITH_CONTEXT, Map.of()))
                .user(render(USER_WITH_CONTEXT, contextModel(question, context)))
                .call()
                .content();
    }

    /** 命中知识库片段时的流式作答（SSE）。 */
    public Flux<String> answerWithContextStream(String question, String context) {
        return getChatClient().prompt()
                .system(render(SYSTEM_WITH_CONTEXT, Map.of()))
                .user(render(USER_WITH_CONTEXT, contextModel(question, context)))
                .stream()
                .content();
    }

    /** 未召回任何片段时的兜底同步作答。 */
    public String answerWithoutContext(String question, String knowledgeBaseNames) {
        return getChatClient().prompt()
                .system(render(SYSTEM_NO_CONTEXT, Map.of()))
                .user(render(USER_NO_CONTEXT, noContextModel(question, knowledgeBaseNames)))
                .call()
                .content();
    }

    /** 未召回任何片段时的兜底流式作答（SSE）。 */
    public Flux<String> answerWithoutContextStream(String question, String knowledgeBaseNames) {
        return getChatClient().prompt()
                .system(render(SYSTEM_NO_CONTEXT, Map.of()))
                .user(render(USER_NO_CONTEXT, noContextModel(question, knowledgeBaseNames)))
                .stream()
                .content();
    }

    private Map<String, Object> contextModel(String question, String context) {
        return Map.of(
                "currentDate", LocalDate.now().toString(),
                "context", context == null ? "" : context,
                "question", question);
    }

    private Map<String, Object> noContextModel(String question, String knowledgeBaseNames) {
        return Map.of(
                "currentDate", LocalDate.now().toString(),
                "knowledgeBaseNames", knowledgeBaseNames == null ? "" : knowledgeBaseNames,
                "question", question);
    }

    private String render(String templatePath, Map<String, Object> variables) {
        return new PromptTemplate(new ClassPathResource(templatePath)).render(variables);
    }

    private ChatClient getChatClient() {
        ChatClient chatClient = chatClientProvider.getIfAvailable();
        if (chatClient == null) {
            throw new IllegalStateException("未配置可用的 AI 模型，请设置 DASHSCOPE_API_KEY");
        }
        return chatClient;
    }
}