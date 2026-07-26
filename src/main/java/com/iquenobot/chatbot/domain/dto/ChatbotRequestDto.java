package com.iquenobot.chatbot.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequestDto {

    @NotBlank(message = "El mensaje es obligatorio")
    private String message;

    private Map<String, Object> context;
}
