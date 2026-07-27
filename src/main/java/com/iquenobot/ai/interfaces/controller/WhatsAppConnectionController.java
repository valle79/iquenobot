package com.iquenobot.ai.interfaces.controller;

import com.iquenobot.ai.application.WhatsAppConnectionService;
import com.iquenobot.shared.domain.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/whatsapp")
@RequiredArgsConstructor
@Tag(name = "WhatsApp Connection", description = "Gestión de conexión de WhatsApp")
public class WhatsAppConnectionController {

    private final WhatsAppConnectionService connectionService;

    @GetMapping("/status")
    @Operation(summary = "Estado de conexión de WhatsApp", description = "Obtiene el estado actual de la conexión con WhatsApp")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<WhatsAppConnectionService.ConnectionStatusResponse>> getStatus() {
        return ResponseEntity.ok(ApiResponse.success(connectionService.getStatus()));
    }

    @PostMapping("/test-connection")
    @Operation(summary = "Probar conexión de WhatsApp", description = "Prueba la conexión con el proveedor de WhatsApp y actualiza el estado")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<WhatsAppConnectionService.TestConnectionResponse>> testConnection() {
        return ResponseEntity.ok(ApiResponse.success(connectionService.testConnection()));
    }
}
