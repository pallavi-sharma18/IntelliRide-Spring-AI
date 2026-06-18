package com.flourish.intelliride.services;

import com.flourish.intelliride.entities.User;
import com.flourish.intelliride.entities.enums.Role;
import com.flourish.intelliride.tools.DriverTools;
import com.flourish.intelliride.tools.RiderTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantService {

    private final ChatClient chatClient;
    private final RiderTools riderTools;
    private final DriverTools driverTools;

    public String chat(String userMessage,String conversationId) {
        log.info("CHAT conversationId={} ", conversationId);
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<Object> tools = new ArrayList<>();
        if (user.getRoles().contains(Role.RIDER))  tools.add(riderTools);
        if (user.getRoles().contains(Role.DRIVER)) tools.add(driverTools);

        return chatClient
                .prompt()
                .user(userMessage)
                .tools(tools.toArray())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
