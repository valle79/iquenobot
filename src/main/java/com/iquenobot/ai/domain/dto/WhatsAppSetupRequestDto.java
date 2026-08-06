package com.iquenobot.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Solicitud de configuración automática de WhatsApp.
 * El usuario final solo indica el proveedor; la instancia, el webhook y
 * las credenciales se crean automáticamente por detrás.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppSetupRequestDto {

    @NotBlank(message = "Selecciona un proveedor")
    private String provider;

    @Size(max = 30, message = "El número de teléfono es demasiado largo")
    private String phoneNumber;
}
