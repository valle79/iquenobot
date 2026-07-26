package com.iquenobot.plan.domain.dto;

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
public class UpdatePlanRequestDto {

    @Size(min = 2, max = 100)
    private String name;

    @Size(min = 2, max = 50)
    private String code;

    @Size(max = 500)
    private String description;

    @PositiveOrZero
    private BigDecimal monthlyPrice;

    @PositiveOrZero
    private BigDecimal yearlyPrice;

    private Integer maxUsers;
    private Integer maxConversations;
    private Integer maxContacts;
    private Integer maxStorageMb;
    private String features;
    private Boolean active;
    private Boolean publicPlan;
    private Integer sortOrder;
}
