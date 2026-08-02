package com.iquenobot.knowledge.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateKnowledgeBaseRequestDto {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 500)
    private String title;

    @NotBlank(message = "El contenido es obligatorio")
    private String content;

    private String sourceType;

    @Size(max = 1000)
    private String sourceUrl;

    @Size(max = 500)
    private String fileUrl;

    /**
     * Texto extraído automáticamente del PDF adjunto. Si es null se conserva el
     * valor actual (para ediciones que no reemplazan el archivo); si se envía
     * vacío se limpia (cuando el PDF fue reemplazado o retirado).
     */
    private String extractedText;

    @Size(max = 500)
    private String tags;
}
