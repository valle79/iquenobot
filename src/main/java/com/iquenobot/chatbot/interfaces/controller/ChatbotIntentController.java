package com.iquenobot.chatbot.interfaces.controller;

import com.iquenobot.chatbot.application.ChatbotIntentService;
import com.iquenobot.chatbot.domain.dto.ChatbotIntentDto;
import com.iquenobot.chatbot.domain.dto.CreateChatbotIntentRequestDto;
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
@RequestMapping("/api/v1/chatbot/intents")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chatbot - Intenciones", description = "Gestión de intenciones del chatbot")
public class ChatbotIntentController {

    private final ChatbotIntentService intentService;

    @GetMapping("/{id}")
    @Operation(summary = "Obtener intención", description = "Obtiene los detalles de una intención específica")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<ChatbotIntentDto>> getById(@PathVariable UUID id) {
        ChatbotIntentDto intent = intentService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(intent));
    }

    @GetMapping
    @Operation(summary = "Listar intenciones", description = "Obtiene la lista paginada de intenciones")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<ChatbotIntentDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<ChatbotIntentDto> intents = intentService.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(intents));
    }

    @PostMapping
    @Operation(summary = "Crear intención", description = "Crea una nueva intención para el chatbot")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<ChatbotIntentDto>> create(
            @Valid @RequestBody CreateChatbotIntentRequestDto request) {
        ChatbotIntentDto intent = intentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(intent, "Intención creada exitosamente"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar intención", description = "Actualiza los datos de una intención existente")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<ChatbotIntentDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateChatbotIntentRequestDto request) {
        ChatbotIntentDto intent = intentService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(intent, "Intención actualizada exitosamente"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar intención", description = "Elimina una intención del chatbot")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        intentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Intención eliminada exitosamente"));
    }

    @GetMapping("/count")
    @Operation(summary = "Contar intenciones", description = "Obtiene el conteo de intenciones")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCount() {
        Map<String, Long> counts = Map.of(
                "total", intentService.getCount(),
                "active", intentService.getActiveCount()
        );
        return ResponseEntity.ok(ApiResponse.success(counts));
    }
}
