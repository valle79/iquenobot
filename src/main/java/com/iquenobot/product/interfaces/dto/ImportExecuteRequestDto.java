package com.iquenobot.product.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportExecuteRequestDto {
    @NotEmpty
    @Valid
    private List<ColumnMappingDto> columnMapping;
}
