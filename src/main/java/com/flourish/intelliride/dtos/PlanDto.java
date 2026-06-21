package com.flourish.intelliride.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanDto {
    private String goal;
    private List<PlanStepDto> steps;
}
