package com.iquenobot.chatbot.domain.dto;

import com.iquenobot.shared.enums.ChatbotFlowTrigger;
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
public class ChatbotFlowDto {

    private UUID id;
    private String name;
    private String description;
    private ChatbotFlowTrigger triggerType;
    private String triggerKeywords;
    private String triggerPattern;
    private String flowConfig;
    private Integer priority;
    private boolean active;
    private boolean useAI;
    private String aiPrompt;
    private String fallbackMessage;
    private Long successCount;
    private Long failureCount;
    private Long executionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
