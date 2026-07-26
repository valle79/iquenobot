package com.iquenobot.chatbot.interfaces.controller;

import com.iquenobot.chatbot.application.ChatbotFlowService;
import com.iquenobot.chatbot.domain.dto.ChatbotFlowDto;
import com.iquenobot.chatbot.domain.dto.CreateChatbotFlowRequestDto;
import com.iquenobot.shared.domain.dto.ApiResponse;
import com.iquenobot.shared.domain.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chatbot/flows")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chatbot - Flujos", description = "Gestión de flujos del chatbot")
public class ChatbotFlowController {

    private final ChatbotFlowService flowService;

    @GetMapping("/{id}")
    @Operation(summary = "Obtener flujo", description = "Obtiene los detalles de un flujo específico")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<ChatbotFlowDto>> getById(@PathVariable UUID id) {
        ChatbotFlowDto flow = flowService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(flow));
    }

    @GetMapping
    @Operation(summary = "Listar flujos", description = "Obtiene la lista paginada de flujos del chatbot")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<ChatbotFlowDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<ChatbotFlowDto> flows = flowService.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(flows));
    }

    @PostMapping
    @Operation(summary = "Crear flujo", description = "Crea un nuevo flujo para el chatbot")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<ChatbotFlowDto>> create(
            @Valid @RequestBody CreateChatbotFlowRequestDto request) {
        ChatbotFlowDto flow = flowService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(flow, "Flujo creado exitosamente"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar flujo", description = "Actualiza los datos de un flujo existente")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<ChatbotFlowDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateChatbotFlowRequestDto request) {
        ChatbotFlowDto flow = flowService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(flow, "Flujo actualizado exitosamente"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar flujo", description = "Elimina un flujo del chatbot")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        flowService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Flujo eliminado exitosamente"));
    }

    @GetMapping("/count")
    @Operation(summary = "Contar flujos", description = "Obtiene el conteo de flujos")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCount() {
        Map<String, Long> counts = Map.of(
                "total", flowService.getCount(),
                "active", flowService.getActiveCount()
        );
        return ResponseEntity.ok(ApiResponse.success(counts));
    }
}
