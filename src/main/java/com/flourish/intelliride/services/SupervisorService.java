package com.flourish.intelliride.services;

import com.flourish.intelliride.agents.Agent;
import com.flourish.intelliride.agents.AgentRequest;
import com.flourish.intelliride.agents.AgentResult;
import com.flourish.intelliride.dtos.ChatResponseDto;
import com.flourish.intelliride.dtos.PendingPlanDto;
import com.flourish.intelliride.dtos.PlanDto;
import com.flourish.intelliride.dtos.PlanStepDto;
import com.flourish.intelliride.dtos.ReasoningStepDto;
import com.flourish.intelliride.entities.User;
import com.flourish.intelliride.entities.enums.AgentType;
import com.flourish.intelliride.entities.enums.Role;
import com.flourish.intelliride.entities.enums.StepStatus;
import com.flourish.intelliride.entities.enums.StepType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class SupervisorService {

    private static final int MAX_STEPS = 8;

    private final List<Agent> agents;            // Spring injects all Agent beans
    private final PlannerService planner;
    private final PendingPlanStore pendingPlans;

    public SupervisorService(List<Agent> agents,
                             PlannerService planner,
                             PendingPlanStore pendingPlans) {
        this.agents = agents;
        this.planner = planner;
        this.pendingPlans = pendingPlans;
    }

    // =====================================================================
    // Entry point
    // =====================================================================
    public ChatResponseDto chat(String message, String conversationId, User user) {
        List<ReasoningStepDto> trace = new ArrayList<>();

        // (1) Resuming a paused plan? Treat this message as the confirmation reply.
        Optional<PendingPlanDto> pending = pendingPlans.get(conversationId);
        if (pending.isPresent()) {
            return handleConfirmation(message, pending.get(), conversationId, user, trace);
        }

        // (2) Simple request -> single-agent route (Phase 1 path).
        if (!needsPlanning(message)) {
            return handleSingle(message, conversationId, user, trace);
        }

        // (3) Complex goal -> plan, then execute.
        PlanDto plan = planner.plan(message, user);
        trace.add(ReasoningStepDto.of(AgentType.PLANNER, StepType.PLAN,
                "Plan with " + plan.getSteps().size() + " step(s): " +
                        plan.getSteps().stream().map(PlanStepDto::getDescription).toList()));
        return executePlan(plan, 0, conversationId, user, trace);
    }

    // =====================================================================
    // Phase 1: single-agent path
    // =====================================================================
    private ChatResponseDto handleSingle(String message, String conversationId,
                                         User user, List<ReasoningStepDto> trace) {
        AgentType target = route(message, user, trace);
        AgentResult result = delegate(target, message, conversationId, user, trace);

        trace.add(ReasoningStepDto.of(AgentType.SUPERVISOR, StepType.SYNTHESIZE,
                "Returning answer from " + result.getAgent()));
        return new ChatResponseDto(result.getOutput(), conversationId, trace);
    }

    private AgentType route(String message, User user, List<ReasoningStepDto> trace) {
        String m = message.toLowerCase();
        AgentType target;

        if (isPolicyQuestion(m)) {
            target = AgentType.SUPPORT;
        } else if (user.getRoles().contains(Role.DRIVER) && isDriverIntent(m)) {
            target = AgentType.DRIVER;
        } else if (user.getRoles().contains(Role.RIDER)) {
            target = AgentType.RIDER;
        } else if (user.getRoles().contains(Role.DRIVER)) {
            target = AgentType.DRIVER;
        } else {
            target = AgentType.SUPPORT;
        }

        trace.add(ReasoningStepDto.of(AgentType.SUPERVISOR, StepType.ROUTE, "Routed to " + target));
        return target;
    }

    // =====================================================================
    // Phase 2: planning, execution loop, resume
    // =====================================================================
    private ChatResponseDto executePlan(PlanDto plan, int startIndex,
                                        String conversationId, User user,
                                        List<ReasoningStepDto> trace) {
        List<PlanStepDto> steps = plan.getSteps();

        // Runaway guard: if the planner produced more steps than we allow, refuse to
        // run a partial plan silently — surface it in the trace and the user-facing reply.
        if (steps.size() > MAX_STEPS) {
            log.warn("Plan has {} steps, exceeding MAX_STEPS={}; refusing to execute.",
                    steps.size(), MAX_STEPS);
            pendingPlans.clear(conversationId);
            trace.add(ReasoningStepDto.of(AgentType.SUPERVISOR, StepType.ERROR,
                    "Plan has " + steps.size() + " steps (limit " + MAX_STEPS + "); too complex to run safely."));
            return new ChatResponseDto(
                    "That request needs more steps than I can safely handle at once (limit "
                            + MAX_STEPS + "). Please break it into smaller requests.",
                    conversationId, trace);
        }

        for (int i = startIndex; i < steps.size(); i++) {
            PlanStepDto step = steps.get(i);

            // Pause for human approval on data-changing steps.
            if (step.isRequiresConfirmation() && !step.isConfirmed()) {
                step.setStatus(StepStatus.NEEDS_CONFIRMATION);
                pendingPlans.save(conversationId, new PendingPlanDto(plan, i));
                trace.add(ReasoningStepDto.of(AgentType.SUPERVISOR, StepType.CONFIRMATION,
                        "Awaiting confirmation for: " + step.getDescription()));
                return new ChatResponseDto(
                        "I'm about to: " + step.getDescription() + ". Shall I proceed? (yes/no)",
                        conversationId, trace);
            }

            step.setStatus(StepStatus.RUNNING);
            String instruction = step.isConfirmed()
                    ? step.getDescription() + " (The user has confirmed; execute the action now.)"
                    : step.getDescription();

            AgentResult r = delegate(step.getTargetAgent(), instruction, conversationId, user, trace);
            step.setResult(r.getOutput());
            step.setStatus(StepStatus.DONE);
        }

        pendingPlans.clear(conversationId);
        trace.add(ReasoningStepDto.of(AgentType.SUPERVISOR, StepType.SYNTHESIZE,
                "All steps complete; synthesizing final answer."));
        return new ChatResponseDto(synthesize(plan), conversationId, trace);
    }

    private ChatResponseDto handleConfirmation(String message, PendingPlanDto pending,
                                               String conversationId, User user,
                                               List<ReasoningStepDto> trace) {
        if (isAffirmative(message)) {
            int idx = pending.getCurrentIndex();
            pending.getPlan().getSteps().get(idx).setConfirmed(true);   // unblock that step
            return executePlan(pending.getPlan(), idx, conversationId, user, trace); // resume here
        }

        pendingPlans.clear(conversationId);
        trace.add(ReasoningStepDto.of(AgentType.SUPERVISOR, StepType.ROUTE,
                "User declined; remaining plan cancelled."));
        return new ChatResponseDto(
                "Okay, I won't do that. I've cancelled the rest of the plan.",
                conversationId, trace);
    }

    // =====================================================================
    // Helpers
    // =====================================================================
    private AgentResult delegate(AgentType type, String message,
                                 String conversationId, User user,
                                 List<ReasoningStepDto> trace) {
        Agent agent = findByType(type);
        AgentRequest req = AgentRequest.builder()
                .message(message)
                .conversationId(conversationId)
                .user(user)
                .trace(trace)
                .build();
        return agent.handle(req);   // agent appends its own DELEGATE / TOOL_CALL steps
    }

    private String synthesize(PlanDto plan) {
        // Simplest version: stitch step results together.
        // Upgrade later to an LLM "summarize these results for the user" call.
        StringBuilder sb = new StringBuilder();
        for (PlanStepDto s : plan.getSteps()) {
            if (s.getResult() != null && !s.getResult().isBlank()) {
                sb.append(s.getResult()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private boolean needsPlanning(String message) {
        String s = message.toLowerCase();
        return s.contains(" then ") || s.contains(" and ") || s.contains(",")
                || s.contains("cheapest") || s.contains("before");
    }

    private boolean isAffirmative(String message) {
        String s = message.trim().toLowerCase();
        return s.startsWith("y") || s.contains("confirm")
                || s.contains("proceed") || s.contains("ok") || s.contains("sure");
    }

    private boolean isPolicyQuestion(String m) {
        return m.contains("policy") || m.contains("refund") || m.contains("how does")
                || m.contains("what is") || m.contains("rules") || m.contains("fee");
    }

    private boolean isDriverIntent(String m) {
        return m.contains("available") || m.contains("online") || m.contains("offline")
                || m.contains("accept") || m.contains("otp") || m.contains("start ride")
                || m.contains("end ride");
    }

    private Agent findByType(AgentType type) {
        return agents.stream()
                .filter(a -> a.type() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No agent for type " + type));
    }
}