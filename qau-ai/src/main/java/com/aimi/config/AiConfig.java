package com.aimi.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "openai", matchIfMissing = true)
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("你是一个智能问答助手，负责根据知识库内容回答用户的问题。请用简洁、准确的语言回答。")
                .build();
    }
}
