package com.iquenobot.chatbot.domain.dto;

import com.iquenobot.shared.enums.ChatbotFlowTrigger;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatbotFlowRequestDto {

    @NotBlank(message = "El nombre del flujo es obligatorio")
    private String name;

    private String description;

    @NotNull(message = "El tipo de disparador es obligatorio")
    private ChatbotFlowTrigger triggerType;

    private String triggerKeywords;
    private String triggerPattern;

    @NotBlank(message = "La configuración del flujo es obligatoria")
    private String flowConfig;

    private Integer priority;
    private boolean active;
    private boolean useAI;
    private String aiPrompt;
    private String fallbackMessage;
}
