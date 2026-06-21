package com.flourish.intelliride.agents;

import com.flourish.intelliride.dtos.ReasoningStepDto;
import com.flourish.intelliride.entities.enums.AgentType;
import com.flourish.intelliride.entities.enums.Role;
import com.flourish.intelliride.entities.enums.StepType;
import com.flourish.intelliride.tools.DriverTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DriverAgent implements Agent {

    private final ChatClient chatClient;

    public DriverAgent(ChatClient.Builder builder, ChatMemory chatMemory, DriverTools driverTools) {
        this.chatClient = builder
                .defaultSystem("""
                        You are the DRIVER assistant for IntelliRide.
                        Help the driver with their profile, availability, accept ride, start with OTP, end ride, rate rider.
                        Action tools (acceptRide,startRide,endRide,cancelRide,rateRider) change data:
                          1) call with confirmed=false to get a preview,
                          2) show the preview and ask the user to confirm,
                          3) only call again with confirmed=true after the user explicitly says yes.
                        Never set confirmed=true on your own. Never invent ride ids — look them up with getMyRides first.
                        """)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(driverTools)   // <-- this agent's fixed tool set
                .build();
    }

    @Override
    public AgentType type() {
        return AgentType.DRIVER;
    }

    @Override
    public boolean supports(AgentRequest request) {
        return request.getUser().getRoles().contains(Role.DRIVER);
    }

    @Override
    public AgentResult handle(AgentRequest request) {
        request.addStep(ReasoningStepDto.of(AgentType.DRIVER, StepType.DELEGATE,
                "DriverAgent handling: " + request.getMessage()));

        String content = chatClient.prompt()
                .user(request.getMessage())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, request.getConversationId()))
                .call()
                .content();

        request.addStep(ReasoningStepDto.of(AgentType.DRIVER, StepType.TOOL_CALL,
                "DriverAgent produced a response"));
        return AgentResult.of(AgentType.DRIVER, content);
    }
}
