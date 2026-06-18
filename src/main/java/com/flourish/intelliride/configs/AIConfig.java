package com.flourish.intelliride.configs;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {
    @Bean
    ChatClient chatClient(ChatClient.Builder builder, VectorStore vectorStore,ChatMemory chatMemory) {
        return builder
                .defaultSystem("""
                        You are the Uber ride assistant.
                        Use the provided context for policy/FAQ questions.
                        Tools:
                        - Read-only tools (getMyProfile, getMyRides, getWalletBalance) may be called
                          freely whenever they help answer the user.
                        - Action tools (requestRide, cancelRide, rateDriver) change data. For these:
                          1) call with confirmed=false to get a preview,
                          2) show the preview and ask the user to confirm,
                          3) only call again with confirmed=true after the user explicitly says yes.
                        Never set confirmed=true on your own initiative. Never invent ride ids —
                        look them up with getMyRides first.
                """)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build(), // short term memory
                        QuestionAnswerAdvisor.builder(vectorStore).build())
                .build();
    }

    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository chatMemoryRepository){

        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();
    }
}
