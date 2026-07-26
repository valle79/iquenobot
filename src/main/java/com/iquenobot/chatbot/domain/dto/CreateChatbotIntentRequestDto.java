package com.iquenobot.chatbot.domain.dto;

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
public class CreateChatbotIntentRequestDto {

    @NotBlank(message = "El nombre de la intención es obligatorio")
    private String intentName;

    private String description;

    @NotBlank(message = "Las frases de entrenamiento son obligatorias")
    private String trainingPhrases;

    @NotBlank(message = "Las respuestas son obligatorias")
    private String responses;

    private String entities;
    private String contextRequired;
    private String contextOutput;
    private String actions;

    @NotNull(message = "El umbral de confianza es obligatorio")
    private Double confidenceThreshold;

    private boolean active;
    private Integer priority;
}
