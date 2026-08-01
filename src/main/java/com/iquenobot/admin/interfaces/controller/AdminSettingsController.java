package com.iquenobot.admin.interfaces.controller;

import com.iquenobot.admin.application.AdminSettingsService;
import com.iquenobot.admin.application.DatabaseBackupService;
import com.iquenobot.admin.domain.dto.BackupInfoDto;
import com.iquenobot.setting.domain.dto.SettingDto;
import com.iquenobot.shared.application.EmailService;
import com.iquenobot.shared.domain.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin Settings", description = "Configuración global del sistema")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;
    private final EmailService emailService;
    private final DatabaseBackupService backupService;

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

    @PostMapping("/smtp/test")
    @Operation(summary = "Enviar correo de prueba para validar la configuración SMTP")
    public ResponseEntity<ApiResponse<Void>> testSmtp(@RequestBody TestEmailRequest request) {
        emailService.sendTestEmail(request.to());
        return ResponseEntity.ok(ApiResponse.success(null, "Correo de prueba enviado correctamente a " + request.to()));
    }

    @GetMapping("/backups")
    @Operation(summary = "Listar respaldos disponibles")
    public ResponseEntity<ApiResponse<List<BackupInfoDto>>> listBackups() {
        return ResponseEntity.ok(ApiResponse.success(backupService.listBackups()));
    }

    @PostMapping("/backups")
    @Operation(summary = "Crear un respaldo manual de la base de datos")
    public ResponseEntity<ApiResponse<BackupInfoDto>> createBackup() {
        BackupInfoDto backup = backupService.createBackup();
        return ResponseEntity.ok(ApiResponse.success(backup, "Respaldo creado correctamente"));
    }

    public record TestEmailRequest(
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "Correo inválido")
            String to) {}
}
