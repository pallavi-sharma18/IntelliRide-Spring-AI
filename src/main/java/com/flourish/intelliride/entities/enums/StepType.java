package com.flourish.intelliride.entities.enums;

public enum StepType {
    ROUTE,         // supervisor decided where to send the request
    DELEGATE,      // supervisor handed off to a sub-agent
    TOOL_CALL,     // an agent invoked a tool
    PLAN,          // planner produced/updated a plan (Phase 2)
    SYNTHESIZE,    // supervisor combined results into final answer
    CONFIRMATION,  // paused for human confirmation (Phase 2)
    ERROR          // something failed
}
