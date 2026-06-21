package com.flourish.intelliride.services;

import com.flourish.intelliride.dtos.PlanDto;
import com.flourish.intelliride.dtos.PlanStepDto;
import com.flourish.intelliride.entities.User;
import com.flourish.intelliride.entities.enums.AgentType;
import com.flourish.intelliride.entities.enums.StepStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PlannerService {

    private final ChatClient chatClient;

    public PlannerService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        You are the PLANNER for IntelliRide, a ride-hailing assistant.
                        Break the user's goal into an ordered list of minimal, concrete steps.

                        For each step:
                        - set targetAgent to exactly one of: RIDER, DRIVER, SUPPORT.
                          RIDER = rider profile/rides/wallet/booking/cancel/rate-driver.
                          DRIVER = availability/accept/start(OTP)/end/cancel/rate-rider.
                          SUPPORT = policy / pricing-rules / FAQ questions (read-only).
                        - set requiresConfirmation=true for any step that CHANGES data
                          (book, cancel, accept, start, end, rate, pay). Read-only steps = false.

                        Rules:
                        - Respect the user's roles; never plan actions a role can't perform.
                        - Keep it to at most 8 steps.
                        - Do NOT add a separate "ask for confirmation" step — that's handled by the flag.
                        """)
                .build();
    }

    public PlanDto plan(String goal, User user) {
        PlanDto plan;
        try {
            plan = chatClient.prompt()
                    .user("User roles: " + user.getRoles() + "\nGoal: " + goal)
                    .call()
                    .entity(PlanDto.class);          // <-- structured output
        } catch (Exception e) {
            // model returned malformed JSON / structured-output conversion failed
            log.warn("Planner failed to produce a structured plan for goal '{}': {}", goal, e.getMessage());
            plan = null;
        }

        // Never return a null/empty plan — fall back to a single SUPPORT step so the
        // caller always has something safe (read-only) to execute.
        if (plan == null || plan.getSteps() == null || plan.getSteps().isEmpty()) {
            PlanDto fallback = new PlanDto();
            fallback.setGoal(goal);
            List<PlanStepDto> steps = new ArrayList<>();
            steps.add(new PlanStepDto(goal, AgentType.SUPPORT, false, false, StepStatus.PENDING, null));
            fallback.setSteps(steps);
            return fallback;
        }

        // normalize runtime fields the model may leave null
        plan.getSteps().forEach(s -> {
            if (s.getStatus() == null) s.setStatus(StepStatus.PENDING);
        });
        return plan;
    }
}