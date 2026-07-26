package com.iquenobot.plan.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlanRequestDto {

    @NotBlank(message = "El nombre del plan es obligatorio")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "El código del plan es obligatorio")
    @Size(min = 2, max = 50)
    private String code;

    @Size(max = 500)
    private String description;

    @NotNull(message = "El precio mensual es obligatorio")
    @PositiveOrZero
    private BigDecimal monthlyPrice;

    @NotNull(message = "El precio anual es obligatorio")
    @PositiveOrZero
    private BigDecimal yearlyPrice;

    private Integer maxUsers;
    private Integer maxConversations;
    private Integer maxContacts;
    private Integer maxStorageMb;
    private String features;
    private boolean active;
    private boolean publicPlan;
    private int sortOrder;
}
