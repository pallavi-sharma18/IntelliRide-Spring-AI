package com.flourish.intelliride.agents;

import com.flourish.intelliride.dtos.ReasoningStepDto;
import com.flourish.intelliride.entities.enums.AgentType;
import com.flourish.intelliride.entities.enums.Role;
import com.flourish.intelliride.entities.enums.StepType;
import com.flourish.intelliride.tools.RiderTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RiderAgent implements Agent {

    private final ChatClient chatClient;

    public RiderAgent(ChatClient.Builder builder, ChatMemory chatMemory, RiderTools riderTools) {
        this.chatClient = builder
                .defaultSystem("""
                        You are the RIDER assistant for IntelliRide.
                        Help the rider with their profile, rides, wallet, booking, cancelling, and rating drivers.
                        Action tools (requestRide, cancelRide, rateDriver) change data:
                          1) call with confirmed=false to get a preview,
                          2) show the preview and ask the user to confirm,
                          3) only call again with confirmed=true after the user explicitly says yes.
                        Never set confirmed=true on your own. Never invent ride ids — look them up with getMyRides first.
                        """)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(riderTools)   // <-- this agent's fixed tool set
                .build();
    }

    @Override
    public AgentType type() {
        return AgentType.RIDER;
    }

    @Override
    public boolean supports(AgentRequest request) {
        return request.getUser().getRoles().contains(Role.RIDER);
    }

    @Override
    public AgentResult handle(AgentRequest request) {
        request.addStep(ReasoningStepDto.of(AgentType.RIDER, StepType.DELEGATE,
                "RiderAgent handling: " + request.getMessage()));

        String content = chatClient.prompt()
                .user(request.getMessage())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, request.getConversationId()))
                .call()
                .content();

        request.addStep(ReasoningStepDto.of(AgentType.RIDER, StepType.TOOL_CALL,
                "RiderAgent produced a response"));
        return AgentResult.of(AgentType.RIDER, content);
    }
}
