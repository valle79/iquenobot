package com.iquenobot.chatbot.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponseDto {

    private String message;
    private String intentDetected;
    private Double confidence;
    private List<String> suggestedActions;
    private Map<String, Object> entities;
    private boolean requiresHumanAgent;
    private String flowExecuted;
}
