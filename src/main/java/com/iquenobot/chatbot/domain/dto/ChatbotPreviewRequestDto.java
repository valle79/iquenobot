package com.iquenobot.chatbot.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Solicitud del modo prueba del chatbot. El contexto es opcional y puede
 * incluir {@code history} (últimos mensajes user/assistant), {@code isFirstMessage}
 * y {@code fallbackCount}, que el frontend mantiene en memoria mientras dura la
 * sesión de prueba. Nada de esta información se persiste en la base de datos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotPreviewRequestDto {

    @NotBlank(message = "El mensaje es obligatorio")
    private String message;

    private Map<String, Object> context;
}