package com.iquenobot.contact.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportContactsRequestDto {
    @NotEmpty(message = "La lista de contactos no puede estar vacía")
    private List<Map<String, String>> contacts;
}
