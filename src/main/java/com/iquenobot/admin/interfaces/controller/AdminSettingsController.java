package com.iquenobot.admin.interfaces.controller;

import com.iquenobot.admin.application.AdminSettingsService;
import com.iquenobot.setting.domain.dto.SettingDto;
import com.iquenobot.shared.domain.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@Tag(name = "Admin Settings", description = "Configuración global del sistema")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;

    @GetMapping("/{category}")
    @Operation(summary = "Obtener configuración global por categoría")
    public ResponseEntity<ApiResponse<List<SettingDto>>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.success(adminSettingsService.getByCategory(category)));
    }

    @PutMapping("/{category}")
    @Operation(summary = "Actualizar configuración global por categoría")
    public ResponseEntity<ApiResponse<List<SettingDto>>> update(
            @PathVariable String category,
            @RequestBody Map<String, String> settings) {
        return ResponseEntity.ok(ApiResponse.success(
                adminSettingsService.updateSettings(category, settings),
                "Configuración actualizada"));
    }
}
