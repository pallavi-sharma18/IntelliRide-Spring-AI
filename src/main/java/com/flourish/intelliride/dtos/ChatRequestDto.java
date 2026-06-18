package com.flourish.intelliride.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequestDto {
    @NotBlank
    @Size(max = 2000, message = "Message too long (max 2000 characters).")
    private String message;
}
