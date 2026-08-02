package com.iquenobot.channel.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWhatsAppChannelRequestDto {

    @Size(max = 100, message = "El nombre del canal no puede exceder 100 caracteres")
    private String channelName;

    @Size(max = 30, message = "El número de teléfono no puede exceder 30 caracteres")
    private String phoneNumber;
}
