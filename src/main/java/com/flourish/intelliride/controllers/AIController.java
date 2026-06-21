package com.flourish.intelliride.controllers;

import com.flourish.intelliride.dtos.ChatRequestDto;
import com.flourish.intelliride.dtos.ChatResponseDto;
import com.flourish.intelliride.entities.User;
import com.flourish.intelliride.services.SupervisorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AIController {
    private final SupervisorService supervisorService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDto> chat(@Valid @RequestBody ChatRequestDto request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String conversationId = "user-" + user.getId();
        ChatResponseDto response = supervisorService.chat(request.getMessage(), conversationId, user);
        return ResponseEntity.ok(response);
    }
}
