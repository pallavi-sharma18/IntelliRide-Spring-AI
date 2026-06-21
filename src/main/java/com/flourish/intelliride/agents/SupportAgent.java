package com.flourish.intelliride.agents;

import com.flourish.intelliride.dtos.ReasoningStepDto;
import com.flourish.intelliride.entities.enums.AgentType;
import com.flourish.intelliride.entities.enums.StepType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class SupportAgent implements Agent {

    private final ChatClient chatClient;

    public SupportAgent(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder
                .defaultSystem("""
                        You are the SUPPORT assistant for IntelliRide.
                        Answer policy, pricing-rules, and FAQ questions using ONLY the provided context.
                        If the answer isn't in the context, say you don't know. Do not take any actions.
                        """)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        QuestionAnswerAdvisor.builder(vectorStore).build())
                .build();
    }

    @Override
    public AgentType type() { return AgentType.SUPPORT; }

    @Override
    public boolean supports(AgentRequest request) { return true; } // fallback / policy intent

    @Override
    public AgentResult handle(AgentRequest request) {
        request.addStep(ReasoningStepDto.of(AgentType.SUPPORT, StepType.DELEGATE,
                "SupportAgent answering from knowledge base"));
        String content = chatClient.prompt().user(request.getMessage()).call().content();
        return AgentResult.of(AgentType.SUPPORT, content);
    }
}