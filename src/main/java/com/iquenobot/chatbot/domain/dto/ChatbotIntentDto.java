package com.iquenobot.chatbot.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotIntentDto {

    private UUID id;
    private String intentName;
    private String description;
    private String trainingPhrases;
    private String responses;
    private String entities;
    private String contextRequired;
    private String contextOutput;
    private String actions;
    private Double confidenceThreshold;
    private boolean active;
    private Integer priority;
    private Long matchedCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
