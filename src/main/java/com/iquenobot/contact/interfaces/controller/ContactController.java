package com.iquenobot.contact.interfaces.controller;

import com.iquenobot.contact.application.ContactService;
import com.iquenobot.contact.domain.dto.ContactDto;
import com.iquenobot.contact.domain.dto.CreateContactRequestDto;
import com.iquenobot.shared.domain.dto.ApiResponse;
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.enums.ContactStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Contactos", description = "Gestión de contactos del CRM")
public class ContactController {

    private final ContactService contactService;

    @GetMapping("/{id}")
    @Operation(summary = "Obtener contacto por ID", description = "Obtiene los detalles de un contacto específico")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<ContactDto>> getById(@PathVariable UUID id) {
        ContactDto contact = contactService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(contact));
    }

    @GetMapping
    @Operation(summary = "Listar contactos", description = "Obtiene la lista paginada de contactos del tenant")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<ContactDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<ContactDto> contacts = contactService.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(contacts));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar contactos", description = "Busca contactos por nombre, email, teléfono o empresa")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<ContactDto>>> search(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<ContactDto> contacts = contactService.searchContacts(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(contacts));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filtrar por estado", description = "Obtiene contactos filtrados por estado")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<ContactDto>>> getByStatus(
            @PathVariable ContactStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<ContactDto> contacts = contactService.getByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(contacts));
    }

    @PostMapping
    @Operation(summary = "Crear contacto", description = "Crea un nuevo contacto en el sistema")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<ContactDto>> create(
            @Valid @RequestBody CreateContactRequestDto request) {
        ContactDto contact = contactService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(contact, "Contacto creado exitosamente"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar contacto", description = "Actualiza los datos de un contacto existente")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<ContactDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateContactRequestDto request) {
        ContactDto contact = contactService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(contact, "Contacto actualizado exitosamente"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar contacto", description = "Elimina un contacto (soft delete)")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        contactService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Contacto eliminado exitosamente"));
    }

    @PostMapping("/{id}/block")
    @Operation(summary = "Bloquear contacto", description = "Bloquea un contacto para que no pueda enviar mensajes")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<Void>> block(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        contactService.blockContact(id, reason);
        return ResponseEntity.ok(ApiResponse.success(null, "Contacto bloqueado exitosamente"));
    }

    @PostMapping("/{id}/unblock")
    @Operation(summary = "Desbloquear contacto", description = "Desbloquea un contacto previamente bloqueado")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<Void>> unblock(@PathVariable UUID id) {
        contactService.unblockContact(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Contacto desbloqueado exitosamente"));
    }
}