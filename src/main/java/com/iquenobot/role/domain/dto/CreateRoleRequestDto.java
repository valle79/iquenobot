package com.iquenobot.role.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequestDto {

    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String name;

    @NotBlank(message = "El nombre mostrado es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre mostrado debe tener entre 2 y 100 caracteres")
    private String displayName;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String description;

    private List<String> permissions;
}
