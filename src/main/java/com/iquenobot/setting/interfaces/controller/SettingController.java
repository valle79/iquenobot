package com.iquenobot.setting.interfaces.controller;

import com.iquenobot.setting.application.SettingService;
import com.iquenobot.setting.domain.dto.SettingDto;
import com.iquenobot.setting.domain.dto.UpdateSettingsRequestDto;
import com.iquenobot.shared.domain.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Configuraciones", description = "Gestión de configuraciones del tenant")
public class SettingController {

    private final SettingService settingService;

    @GetMapping
    @Operation(summary = "Obtener configuraciones", description = "Obtiene todas las configuraciones del tenant")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<List<SettingDto>>> getAll() {
        List<SettingDto> settings = settingService.getAll();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @GetMapping("/{category}")
    @Operation(summary = "Obtener configuraciones por categoría", description = "Obtiene las configuraciones filtradas por categoría")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<List<SettingDto>>> getByCategory(@PathVariable String category) {
        List<SettingDto> settings = settingService.getByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping
    @Operation(summary = "Actualizar configuraciones", description = "Actualiza configuraciones en lote para una categoría")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<List<SettingDto>>> update(
            @Valid @RequestBody UpdateSettingsRequestDto request) {
        List<SettingDto> settings = settingService.updateSettings(request);
        return ResponseEntity.ok(ApiResponse.success(settings, "Configuraciones actualizadas exitosamente"));
    }
}
