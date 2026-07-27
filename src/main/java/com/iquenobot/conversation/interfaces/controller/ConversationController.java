package com.iquenobot.conversation.interfaces.controller;

import com.iquenobot.conversation.application.ConversationService;
import com.iquenobot.conversation.domain.dto.ConversationDto;
import com.iquenobot.conversation.domain.dto.ConversationMessageDto;
import com.iquenobot.conversation.domain.dto.CreateConversationRequestDto;
import com.iquenobot.conversation.domain.dto.SendMessageRequestDto;
import com.iquenobot.shared.domain.dto.ApiResponse;
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.enums.ConversationStatus;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Conversaciones", description = "Gestión de conversaciones y mensajes")
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping("/{id}")
    @Operation(summary = "Obtener conversación", description = "Obtiene los detalles de una conversación específica")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<ConversationDto>> getById(@PathVariable UUID id) {
        ConversationDto conversation = conversationService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    @GetMapping
    @Operation(summary = "Listar conversaciones", description = "Obtiene la lista paginada de conversaciones")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<ConversationDto>>> getAll(
            @PageableDefault(size = 20, sort = "lastMessageAt") Pageable pageable) {
        PagedResponse<ConversationDto> conversations = conversationService.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @GetMapping("/active")
    @Operation(summary = "Conversaciones activas", description = "Obtiene conversaciones en estado abierto, en progreso o pendiente")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<ConversationDto>>> getActive(
            @PageableDefault(size = 20, sort = "lastMessageAt") Pageable pageable) {
        PagedResponse<ConversationDto> conversations = conversationService.getActiveConversations(pageable);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @GetMapping("/unassigned")
    @Operation(summary = "Conversaciones sin asignar", description = "Obtiene conversaciones que no tienen agente asignado")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<PagedResponse<ConversationDto>>> getUnassigned(
            @PageableDefault(size = 20, sort = "lastMessageAt") Pageable pageable) {
        PagedResponse<ConversationDto> conversations = conversationService.getUnassignedConversations(pageable);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @GetMapping("/my-conversations")
    @Operation(summary = "Mis conversaciones", description = "Obtiene las conversaciones asignadas al usuario actual")
    @PreAuthorize("hasAnyRole('AGENT', 'SUPERVISOR', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<ConversationDto>>> getMyConversations(
            @PageableDefault(size = 20, sort = "lastMessageAt") Pageable pageable) {
        PagedResponse<ConversationDto> conversations = conversationService.getMyConversations(pageable);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filtrar por estado", description = "Obtiene conversaciones filtradas por estado")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<ConversationDto>>> getByStatus(
            @PathVariable ConversationStatus status,
            @PageableDefault(size = 20, sort = "lastMessageAt") Pageable pageable) {
        PagedResponse<ConversationDto> conversations = conversationService.getByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Obtener mensajes", description = "Obtiene los mensajes de una conversación")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<ConversationMessageDto>>> getMessages(
            @PathVariable UUID id,
            @PageableDefault(size = 50, sort = "sentAt") Pageable pageable) {
        PagedResponse<ConversationMessageDto> messages = conversationService.getMessages(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PostMapping
    @Operation(summary = "Crear conversación", description = "Crea una nueva conversación con un contacto")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<ConversationDto>> create(
            @Valid @RequestBody CreateConversationRequestDto request) {
        ConversationDto conversation = conversationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(conversation, "Conversación creada exitosamente"));
    }

    @PostMapping("/messages")
    @Operation(summary = "Enviar mensaje", description = "Envía un mensaje en una conversación")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<ConversationMessageDto>> sendMessage(
            @Valid @RequestBody SendMessageRequestDto request) {
        ConversationMessageDto message = conversationService.sendMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, "Mensaje enviado exitosamente"));
    }

    @PutMapping("/{id}/assign/{userId}")
    @Operation(summary = "Asignar conversación", description = "Asigna una conversación a un agente")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<Void>> assign(
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        conversationService.assignConversation(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Conversación asignada exitosamente"));
    }

    @PutMapping("/{id}/unassign")
    @Operation(summary = "Desasignar conversación", description = "Quita la asignación de una conversación")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<Void>> unassign(@PathVariable UUID id) {
        conversationService.unassignConversation(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Conversación desasignada exitosamente"));
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "Resolver conversación", description = "Marca una conversación como resuelta")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<Void>> resolve(@PathVariable UUID id) {
        conversationService.resolveConversation(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Conversación resuelta exitosamente"));
    }

    @PutMapping("/{id}/close")
    @Operation(summary = "Cerrar conversación", description = "Cierra una conversación")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<Void>> close(@PathVariable UUID id) {
        conversationService.closeConversation(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Conversación cerrada exitosamente"));
    }

    @PutMapping("/{id}/reopen")
    @Operation(summary = "Reabrir conversación", description = "Reabre una conversación cerrada")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<Void>> reopen(@PathVariable UUID id) {
        conversationService.reopenConversation(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Conversación reabierta exitosamente"));
    }

    @PutMapping("/{id}/mark-as-read")
    @Operation(summary = "Marcar como leído", description = "Marca todos los mensajes de una conversación como leídos")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
        conversationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Mensajes marcados como leídos"));
    }

    @PutMapping("/{id}/metadata")
    @Operation(summary = "Actualizar metadata", description = "Actualiza la metadata de una conversación (notas internas)")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<Void>> updateMetadata(
            @PathVariable UUID id,
            @RequestBody String metadata) {
        conversationService.updateMetadata(id, metadata);
        return ResponseEntity.ok(ApiResponse.success(null, "Metadata actualizada"));
    }
}