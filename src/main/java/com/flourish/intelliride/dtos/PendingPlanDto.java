package com.flourish.intelliride.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PendingPlanDto {
    private PlanDto plan;
    private int currentIndex;   // the step awaiting confirmation
}
