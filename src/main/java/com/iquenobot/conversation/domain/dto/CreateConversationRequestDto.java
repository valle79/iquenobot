package com.iquenobot.conversation.domain.dto;

import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.ConversationPriority;
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
public class CreateConversationRequestDto {

    @NotNull(message = "El ID del contacto es obligatorio")
    private UUID contactId;

    @NotNull(message = "El canal es obligatorio")
    private ChannelType channel;

    @Size(max = 200, message = "El asunto no puede exceder 200 caracteres")
    private String subject;

    private ConversationPriority priority;

    private UUID assignedUserId;

    @Size(max = 100, message = "El ID de conversación del canal no puede exceder 100 caracteres")
    private String channelConversationId;

    private String tags;
    
    private String initialMessage;
}