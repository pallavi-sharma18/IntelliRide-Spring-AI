package com.flourish.intelliride.agents;

import com.flourish.intelliride.entities.enums.AgentType;

public interface Agent {
    AgentType type();

    /** Quick check the supervisor can use for rule-based routing. */
    boolean supports(AgentRequest request);

    /** Do the work and return the result; append reasoning to request.getTrace(). */
    AgentResult handle(AgentRequest request);
}
