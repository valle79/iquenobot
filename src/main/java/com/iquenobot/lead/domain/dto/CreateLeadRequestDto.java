package com.iquenobot.lead.domain.dto;

import com.iquenobot.shared.enums.LeadSource;
import com.iquenobot.shared.enums.LeadStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLeadRequestDto {

    @NotNull(message = "El contacto es obligatorio")
    private UUID contactId;

    @NotBlank(message = "El título es obligatorio")
    @Size(min = 2, max = 200, message = "El título debe tener entre 2 y 200 caracteres")
    private String title;

    @Size(max = 5000, message = "La descripción no puede exceder 5000 caracteres")
    private String description;

    @NotNull(message = "El estado es obligatorio")
    private LeadStatus status;

    @NotNull(message = "La fuente es obligatoria")
    private LeadSource source;

    @Size(max = 500, message = "Los detalles de fuente no pueden exceder 500 caracteres")
    private String sourceDetails;

    @DecimalMin(value = "0.0", message = "El valor estimado debe ser mayor o igual a 0")
    private BigDecimal estimatedValue;

    @DecimalMin(value = "0.0", message = "La probabilidad debe estar entre 0 y 100")
    @DecimalMax(value = "100.0", message = "La probabilidad debe estar entre 0 y 100")
    private BigDecimal probability;

    @Min(value = 0, message = "El score debe estar entre 0 y 100")
    @Max(value = 100, message = "El score debe estar entre 0 y 100")
    private Integer score;

    private UUID assignedToUserId;

    private LocalDateTime expectedCloseDate;

    @Size(max = 500, message = "Los tags no pueden exceder 500 caracteres")
    private String tags;

    @Size(max = 5000, message = "Las notas no pueden exceder 5000 caracteres")
    private String notes;
}
