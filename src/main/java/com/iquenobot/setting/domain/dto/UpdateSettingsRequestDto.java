package com.iquenobot.setting.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSettingsRequestDto {

    @NotBlank(message = "La categoría es obligatoria")
    private String category;

    private List<SettingEntry> settings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettingEntry {
        @NotBlank(message = "La clave es obligatoria")
        private String key;

        private String value;

        private String type;
    }
}
