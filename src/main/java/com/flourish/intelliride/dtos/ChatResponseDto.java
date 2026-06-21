package com.flourish.intelliride.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponseDto {
    private String reply;
    private String conversationId;
    private List<ReasoningStepDto> reasoning;
}
