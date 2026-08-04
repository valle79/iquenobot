package com.iquenobot.chatbot.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa una acción que el orquestador ejecutaría en producción ante la
 * respuesta del bot, mostrada únicamente como información en el modo prueba.
 * Nunca se ejecuta ni se persiste.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulatedActionDto {

    private String actionType;
    private String label;
    private String description;
}