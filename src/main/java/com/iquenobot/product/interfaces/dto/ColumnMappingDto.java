package com.iquenobot.product.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnMappingDto {
    @NotBlank
    private String fileColumn;
    @NotBlank
    private String productField;
}
