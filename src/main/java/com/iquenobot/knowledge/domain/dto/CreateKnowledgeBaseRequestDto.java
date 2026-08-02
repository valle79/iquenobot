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
public class CreateKnowledgeBaseRequestDto {

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
     * Texto extraído automáticamente del PDF adjunto. Solo uso interno del bot,
     * nunca se expone al cliente final.
     */
    private String extractedText;

    @Size(max = 500)
    private String tags;
}
