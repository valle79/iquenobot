package com.iquenobot.channel.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWhatsAppChannelRequestDto {

    @NotBlank(message = "El nombre del canal es obligatorio")
    @Size(max = 100, message = "El nombre del canal no puede exceder 100 caracteres")
    private String channelName;

    @NotBlank(message = "El nombre de instancia es obligatorio")
    @Size(max = 120, message = "El nombre de instancia no puede exceder 120 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-]+$", message = "El nombre de instancia solo admite letras, números, guiones y guiones bajos")
    private String instanceName;

    @Size(max = 30, message = "El número de teléfono no puede exceder 30 caracteres")
    private String phoneNumber;
}
