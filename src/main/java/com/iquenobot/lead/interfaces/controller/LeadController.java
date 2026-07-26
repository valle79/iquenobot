package com.iquenobot.lead.interfaces.controller;

import com.iquenobot.lead.application.LeadService;
import com.iquenobot.lead.domain.dto.CreateLeadRequestDto;
import com.iquenobot.lead.domain.dto.LeadDto;
import com.iquenobot.shared.domain.dto.ApiResponse;
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.enums.LeadSource;
import com.iquenobot.shared.enums.LeadStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
@Tag(name = "Leads", description = "Endpoints para gestión de leads")
@SecurityRequirement(name = "bearerAuth")
public class LeadController {

    private final LeadService leadService;

    @GetMapping("/{id}")
    @Operation(summary = "Obtener lead por ID")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<LeadDto>> getById(@PathVariable UUID id) {
        LeadDto lead = leadService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(lead));
    }

    @GetMapping
    @Operation(summary = "Obtener todos los leads paginados")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<LeadDto>>> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<LeadDto> leads = leadService.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(leads));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Obtener leads por estado")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<LeadDto>>> getByStatus(
            @PathVariable LeadStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<LeadDto> leads = leadService.getByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(leads));
    }

    @GetMapping("/source/{source}")
    @Operation(summary = "Obtener leads por fuente")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<LeadDto>>> getBySource(
            @PathVariable LeadSource source,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<LeadDto> leads = leadService.getBySource(source, pageable);
        return ResponseEntity.ok(ApiResponse.success(leads));
    }

    @GetMapping("/assigned/{userId}")
    @Operation(summary = "Obtener leads asignados a un usuario")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<LeadDto>>> getByAssignedUser(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<LeadDto> leads = leadService.getByAssignedUser(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(leads));
    }

    @GetMapping("/unassigned")
    @Operation(summary = "Obtener leads sin asignar")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<PagedResponse<LeadDto>>> getUnassignedLeads(
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<LeadDto> leads = leadService.getUnassignedLeads(pageable);
        return ResponseEntity.ok(ApiResponse.success(leads));
    }

    @GetMapping("/high-score")
    @Operation(summary = "Obtener leads con alto score")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<List<LeadDto>>> getHighScoreLeads(
            @RequestParam(defaultValue = "70") Integer minScore) {
        List<LeadDto> leads = leadService.getHighScoreLeads(minScore);
        return ResponseEntity.ok(ApiResponse.success(leads));
    }

    @GetMapping("/stale")
    @Operation(summary = "Obtener leads sin actividad reciente")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<List<LeadDto>>> getStaleLeads(
            @RequestParam(defaultValue = "7") Integer days) {
        List<LeadDto> leads = leadService.getStaleLeads(days);
        return ResponseEntity.ok(ApiResponse.success(leads));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar leads")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<LeadDto>>> search(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<LeadDto> leads = leadService.searchLeads(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(leads));
    }

    @PostMapping
    @Operation(summary = "Crear lead")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<LeadDto>> create(@Valid @RequestBody CreateLeadRequestDto request) {
        LeadDto lead = leadService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(lead, "Lead creado exitosamente"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar lead")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<LeadDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateLeadRequestDto request) {
        LeadDto lead = leadService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(lead, "Lead actualizado exitosamente"));
    }

    @PutMapping("/{id}/assign/{userId}")
    @Operation(summary = "Asignar lead a usuario")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<LeadDto>> assignToUser(
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        LeadDto lead = leadService.assignToUser(id, userId);
        return ResponseEntity.ok(ApiResponse.success(lead, "Lead asignado exitosamente"));
    }

    @PutMapping("/{id}/contacted")
    @Operation(summary = "Marcar lead como contactado")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<LeadDto>> markAsContacted(@PathVariable UUID id) {
        LeadDto lead = leadService.markAsContacted(id);
        return ResponseEntity.ok(ApiResponse.success(lead, "Lead marcado como contactado"));
    }

    @PutMapping("/{id}/qualified")
    @Operation(summary = "Marcar lead como calificado")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<LeadDto>> markAsQualified(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer score) {
        LeadDto lead = leadService.markAsQualified(id, score);
        return ResponseEntity.ok(ApiResponse.success(lead, "Lead marcado como calificado"));
    }

    @PutMapping("/{id}/converted")
    @Operation(summary = "Marcar lead como convertido")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<LeadDto>> markAsConverted(
            @PathVariable UUID id,
            @RequestParam String contactId) {
        LeadDto lead = leadService.markAsConverted(id, contactId);
        return ResponseEntity.ok(ApiResponse.success(lead, "Lead marcado como convertido"));
    }

    @PutMapping("/{id}/lost")
    @Operation(summary = "Marcar lead como perdido")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<LeadDto>> markAsLost(
            @PathVariable UUID id,
            @RequestParam String reason) {
        LeadDto lead = leadService.markAsLost(id, reason);
        return ResponseEntity.ok(ApiResponse.success(lead, "Lead marcado como perdido"));
    }

    @PutMapping("/{id}/disqualified")
    @Operation(summary = "Marcar lead como descalificado")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<LeadDto>> markAsDisqualified(
            @PathVariable UUID id,
            @RequestParam String reason) {
        LeadDto lead = leadService.markAsDisqualified(id, reason);
        return ResponseEntity.ok(ApiResponse.success(lead, "Lead marcado como descalificado"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar lead")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        leadService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Lead eliminado exitosamente"));
    }
}
