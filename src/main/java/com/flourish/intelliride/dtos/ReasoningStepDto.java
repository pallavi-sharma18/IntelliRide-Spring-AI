package com.flourish.intelliride.dtos;

import com.flourish.intelliride.entities.enums.AgentType;
import com.flourish.intelliride.entities.enums.StepType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReasoningStepDto {
    private AgentType agent;       // who took this step
    private StepType type;         // what kind of step
    private String detail;         // human-readable: "Routed to SupportAgent (policy question)"
    private LocalDateTime timestamp;

    public static ReasoningStepDto of(AgentType agent, StepType type, String detail) {
        return new ReasoningStepDto(agent, type, detail, LocalDateTime.now());
    }
}
