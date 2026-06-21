package com.flourish.intelliride.agents;
import com.flourish.intelliride.entities.enums.AgentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AgentResult {
    private AgentType agent;            // who produced this
    private String output;             // the agent's text answer
    private boolean needsConfirmation; // reserved for Phase 2; default false for now

    public static AgentResult of(AgentType agent, String output) {
        return new AgentResult(agent, output, false);
    }
}
