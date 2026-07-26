package com.iquenobot.role.interfaces.controller;

import com.iquenobot.role.application.RoleService;
import com.iquenobot.role.domain.dto.CreateRoleRequestDto;
import com.iquenobot.role.domain.dto.RoleDto;
import com.iquenobot.role.domain.dto.UpdateRoleRequestDto;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Roles", description = "Gestión de roles y permisos")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "Listar roles", description = "Obtiene la lista paginada de roles del tenant")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<PagedResponse<RoleDto>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<RoleDto> roles = roleService.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener rol por ID", description = "Obtiene los detalles de un rol específico")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<RoleDto>> getById(@PathVariable UUID id) {
        RoleDto role = roleService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    @PostMapping
    @Operation(summary = "Crear rol", description = "Crea un nuevo rol personalizado en el tenant")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<RoleDto>> create(@Valid @RequestBody CreateRoleRequestDto request) {
        RoleDto role = roleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(role, "Rol creado exitosamente"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar rol", description = "Actualiza los permisos y datos de un rol")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<RoleDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequestDto request) {
        RoleDto role = roleService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(role, "Rol actualizado exitosamente"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar rol", description = "Elimina un rol personalizado (soft delete)")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        roleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Rol eliminado exitosamente"));
    }
}
