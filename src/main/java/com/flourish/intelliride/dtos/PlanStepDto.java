package com.flourish.intelliride.dtos;

import com.flourish.intelliride.entities.enums.AgentType;
import com.flourish.intelliride.entities.enums.StepStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlanStepDto {
    private String description;          // e.g. "Check the rider's wallet balance"
    private AgentType targetAgent;       // RIDER / DRIVER / SUPPORT  (set by the planner)
    private boolean requiresConfirmation;// true for data-changing steps (set by the planner)
    private boolean confirmed;           // runtime flag; set true after the human approves
    private StepStatus status;           // runtime; starts PENDING
    private String result;               // runtime; the agent's output for this step
}
