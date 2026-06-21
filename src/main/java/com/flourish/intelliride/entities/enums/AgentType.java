package com.flourish.intelliride.entities.enums;

public enum AgentType {
    SUPERVISOR,   // the router/orchestrator
    RIDER,        // wraps RiderTools
    DRIVER,       // wraps DriverTools
    SUPPORT,      // RAG-only, policy/FAQ
    PLANNER
}
