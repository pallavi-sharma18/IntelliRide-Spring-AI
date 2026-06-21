package com.flourish.intelliride.services;

import com.flourish.intelliride.dtos.PendingPlanDto;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PendingPlanStore {
    private final Map<String, PendingPlanDto> store = new ConcurrentHashMap<>();

    public Optional<PendingPlanDto> get(String conversationId) {
        return Optional.ofNullable(store.get(conversationId));
    }
    public void save(String conversationId, PendingPlanDto pending) {
        store.put(conversationId, pending);
    }
    public void clear(String conversationId) {
        store.remove(conversationId);
    }
}