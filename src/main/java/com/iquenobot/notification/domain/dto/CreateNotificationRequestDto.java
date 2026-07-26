package com.iquenobot.notification.domain.dto;

import com.iquenobot.shared.enums.NotificationChannel;
import com.iquenobot.shared.enums.NotificationPriority;
import com.iquenobot.shared.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequestDto {

    @NotNull(message = "El usuario es obligatorio")
    private UUID userId;

    @NotNull(message = "El tipo es obligatorio")
    private NotificationType type;

    @NotNull(message = "El canal es obligatorio")
    private NotificationChannel channel;

    @NotNull(message = "La prioridad es obligatoria")
    private NotificationPriority priority;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200, message = "El título no puede exceder 200 caracteres")
    private String title;

    @NotBlank(message = "El mensaje es obligatorio")
    @Size(max = 5000, message = "El mensaje no puede exceder 5000 caracteres")
    private String message;

    @Size(max = 500, message = "La URL de acción no puede exceder 500 caracteres")
    private String actionUrl;

    @Size(max = 100, message = "La etiqueta de acción no puede exceder 100 caracteres")
    private String actionLabel;

    @Size(max = 100, message = "El ícono no puede exceder 100 caracteres")
    private String icon;

    @Size(max = 500, message = "La URL de imagen no puede exceder 500 caracteres")
    private String imageUrl;

    private LocalDateTime scheduledAt;

    private LocalDateTime expiresAt;

    @Size(max = 50, message = "El tipo de entidad no puede exceder 50 caracteres")
    private String relatedEntityType;

    @Size(max = 255, message = "El ID de entidad no puede exceder 255 caracteres")
    private String relatedEntityId;

    private String metadata;
}
