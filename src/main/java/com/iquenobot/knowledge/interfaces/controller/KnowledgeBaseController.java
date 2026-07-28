package com.iquenobot.knowledge.interfaces.controller;

import com.iquenobot.knowledge.application.KnowledgeBaseService;
import com.iquenobot.knowledge.domain.dto.CreateKnowledgeBaseRequestDto;
import com.iquenobot.knowledge.domain.dto.KnowledgeBaseDto;
import com.iquenobot.knowledge.domain.dto.UpdateKnowledgeBaseRequestDto;
import com.iquenobot.shared.domain.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/knowledge-base")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Base de Conocimiento", description = "Gestión de la base de conocimiento del bot")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping
    @Operation(summary = "Listar entradas", description = "Obtiene todas las entradas de la base de conocimiento")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<List<KnowledgeBaseDto>>> getAll() {
        List<KnowledgeBaseDto> entries = knowledgeBaseService.getAll();
        return ResponseEntity.ok(ApiResponse.success(entries));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar entradas", description = "Busca entradas en la base de conocimiento por texto")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<List<KnowledgeBaseDto>>> search(
            @RequestParam("q") String query) {
        List<KnowledgeBaseDto> entries = knowledgeBaseService.search(query);
        return ResponseEntity.ok(ApiResponse.success(entries));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener entrada", description = "Obtiene una entrada por ID")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<KnowledgeBaseDto>> getById(@PathVariable UUID id) {
        KnowledgeBaseDto entry = knowledgeBaseService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(entry));
    }

    @PostMapping
    @Operation(summary = "Crear entrada", description = "Crea una nueva entrada en la base de conocimiento")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<KnowledgeBaseDto>> create(
            @Valid @RequestBody CreateKnowledgeBaseRequestDto request) {
        KnowledgeBaseDto entry = knowledgeBaseService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(entry, "Entrada creada exitosamente"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar entrada", description = "Actualiza una entrada existente")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<KnowledgeBaseDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateKnowledgeBaseRequestDto request) {
        KnowledgeBaseDto entry = knowledgeBaseService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(entry, "Entrada actualizada exitosamente"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar entrada", description = "Elimina lógicamente una entrada")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        knowledgeBaseService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Entrada eliminada exitosamente"));
    }
}
