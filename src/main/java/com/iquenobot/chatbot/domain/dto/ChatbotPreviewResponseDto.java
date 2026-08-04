package com.iquenobot.chatbot.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta del modo prueba del chatbot. Replica fielmente la respuesta que
 * produciría el bot en producción (misma lógica, mismos intents, flujos, KB,
 * catálogo y llamadas al LLM), y además informa qué acciones ejecutaría el
 * orquestador, sin llegar a ejecutarlas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotPreviewResponseDto {

    private String message;
    private String intentDetected;
    private Double confidence;
    private boolean requiresHumanAgent;
    private boolean requiresClarification;
    private String flowExecuted;
    private List<SimulatedActionDto> simulatedActions;
    private boolean botAvailable;
}