package com.flourish.intelliride.agents;

import com.flourish.intelliride.dtos.ReasoningStepDto;
import com.flourish.intelliride.entities.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class AgentRequest {
    private String message;            // the user's message (or a single plan-step instruction in Phase 2)
    private String conversationId;     // "user-" + user.getId()
    private User user;                 // authenticated principal -> gives roles + identity
    private List<ReasoningStepDto> trace; // SHARED, mutable; agents append here

    // convenience so callers can append without null checks
    public void addStep(ReasoningStepDto step) {
        if (trace == null) {
            trace = new ArrayList<>();
        }
        trace.add(step);
    }
}
