package com.iquenobot.channel.interfaces.controller;

import com.iquenobot.channel.application.WhatsAppChannelService;
import com.iquenobot.channel.domain.dto.CreateWhatsAppChannelRequestDto;
import com.iquenobot.channel.domain.dto.UpdateWhatsAppChannelRequestDto;
import com.iquenobot.channel.domain.dto.WhatsAppChannelDto;
import com.iquenobot.shared.domain.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/whatsapp/channels")
@RequiredArgsConstructor
@Tag(name = "WhatsApp Channels", description = "Gestión de canales de WhatsApp (múltiples números por tenant)")
public class WhatsAppChannelController {

    private final WhatsAppChannelService channelService;

    @GetMapping
    @Operation(summary = "Listar canales de WhatsApp", description = "Obtiene todos los canales de WhatsApp del tenant")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<List<WhatsAppChannelDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(channelService.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener canal por ID", description = "Obtiene un canal de WhatsApp por su ID")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<WhatsAppChannelDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(channelService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Crear canal de WhatsApp", description = "Registra un nuevo número/instancia de WhatsApp para el tenant")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<WhatsAppChannelDto>> create(@Valid @RequestBody CreateWhatsAppChannelRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(channelService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar canal de WhatsApp", description = "Actualiza nombre o número de teléfono del canal")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<WhatsAppChannelDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWhatsAppChannelRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(channelService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar canal de WhatsApp", description = "Elimina (soft delete) un canal de WhatsApp")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        channelService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Canal de WhatsApp eliminado"));
    }

    @PostMapping("/{id}/test-connection")
    @Operation(summary = "Probar conexión del canal", description = "Configura el webhook y verifica el estado de conexión en Evolution API")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<WhatsAppChannelService.TestConnectionResponse>> testConnection(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(channelService.testConnection(id)));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Estado de conexión del canal", description = "Obtiene el estado actual del canal en Evolution API")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<WhatsAppChannelService.ConnectionStatusResponse>> getStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(channelService.getStatus(id)));
    }

    @GetMapping("/{id}/qr-code")
    @Operation(summary = "Obtener código QR del canal", description = "Obtiene el QR para conectar el número escaneando con el teléfono")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<WhatsAppChannelService.QRCodeResponse>> getQRCode(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(channelService.getQRCode(id)));
    }

    @PostMapping("/{id}/disconnect")
    @Operation(summary = "Desconectar canal", description = "Desconecta la instancia de WhatsApp del canal")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<Void>> disconnect(@PathVariable UUID id) {
        channelService.disconnect(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Canal de WhatsApp desconectado"));
    }
}
