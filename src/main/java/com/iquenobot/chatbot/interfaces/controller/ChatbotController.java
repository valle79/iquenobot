package com.iquenobot.chatbot.interfaces.controller;

import com.iquenobot.chatbot.application.ChatbotService;
import com.iquenobot.chatbot.domain.dto.ChatbotRequestDto;
import com.iquenobot.chatbot.domain.dto.ChatbotResponseDto;
import com.iquenobot.shared.domain.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
@Tag(name = "Chatbot", description = "Endpoints para el bot conversacional")
@SecurityRequirement(name = "bearerAuth")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/message")
    @Operation(summary = "Procesar mensaje del chatbot")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT', 'BOT')")
    public ResponseEntity<ApiResponse<ChatbotResponseDto>> processMessage(
            @Valid @RequestBody ChatbotRequestDto request) {
        
        ChatbotResponseDto response = chatbotService.processMessage(
                request.getMessage(),
                request.getContext() != null ? request.getContext() : new HashMap<>()
        );
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
