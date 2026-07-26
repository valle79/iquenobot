package com.iquenobot.conversation.domain.dto;

import com.iquenobot.shared.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequestDto {

    @NotNull(message = "El ID de la conversación es obligatorio")
    private UUID conversationId;

    @NotNull(message = "El tipo de mensaje es obligatorio")
    private MessageType type;

    @NotBlank(message = "El contenido es obligatorio")
    @Size(max = 4096, message = "El contenido no puede exceder 4096 caracteres")
    private String content;

    private String replyToMessageId;

    private String[] attachmentUrls;
}