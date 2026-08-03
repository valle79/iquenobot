package com.iquenobot.admin.interfaces.controller;

import com.iquenobot.admin.application.AdminService;
import com.iquenobot.admin.domain.dto.SystemStatsDto;
import com.iquenobot.admin.domain.dto.TenantUniquenessDto;
import com.iquenobot.admin.domain.dto.UpdateTenantRequestDto;
import com.iquenobot.auth.domain.dto.CreateTenantRequestDto;
import com.iquenobot.auth.domain.dto.TenantDto;
import com.iquenobot.plan.domain.dto.CreatePlanRequestDto;
import com.iquenobot.plan.domain.dto.PlanDto;
import com.iquenobot.plan.domain.dto.UpdatePlanRequestDto;
import com.iquenobot.shared.domain.dto.ApiResponse;
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.enums.TenantStatus;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Endpoints exclusivos para SUPER_ADMIN")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // ========== TENANTS ==========

    @GetMapping("/tenants")
    @Operation(summary = "Listar empresas")
    public ResponseEntity<ApiResponse<PagedResponse<TenantDto>>> getTenants(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getTenants(search, status, pageable)));
    }

    @GetMapping("/tenants/{id}")
    @Operation(summary = "Obtener empresa")
    public ResponseEntity<ApiResponse<TenantDto>> getTenant(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getTenantById(id)));
    }

    @GetMapping("/tenants/check-unique")
    @Operation(summary = "Verificar unicidad de nombre, subdominio y sitio web",
            description = "Devuelve si el nombre de empresa, subdominio y sitio web están disponibles")
    public ResponseEntity<ApiResponse<TenantUniquenessDto>> checkTenantUniqueness(
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String subdomain,
            @RequestParam(required = false) String websiteUrl) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.checkTenantUniqueness(companyName, subdomain, websiteUrl)));
    }

    @PostMapping("/tenants")
    @Operation(summary = "Crear empresa con aprovisionamiento completo")
    public ResponseEntity<ApiResponse<TenantDto>> createTenant(@Valid @RequestBody CreateTenantRequestDto request) {
        TenantDto tenant = adminService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tenant, "Empresa creada exitosamente"));
    }

    @PutMapping("/tenants/{id}")
    @Operation(summary = "Actualizar empresa")
    public ResponseEntity<ApiResponse<TenantDto>> updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateTenant(id, request), "Empresa actualizada"));
    }

    @PutMapping("/tenants/{id}/status")
    @Operation(summary = "Cambiar estado de empresa")
    public ResponseEntity<ApiResponse<TenantDto>> updateTenantStatus(
            @PathVariable UUID id,
            @RequestParam TenantStatus status) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateTenantStatus(id, status), "Estado actualizado"));
    }

    @DeleteMapping("/tenants/{id}")
    @Operation(summary = "Eliminar empresa (soft-delete)")
    public ResponseEntity<ApiResponse<Void>> deleteTenant(@PathVariable UUID id) {
        adminService.deleteTenant(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Empresa eliminada exitosamente"));
    }

    // ========== PLANS ==========

    @GetMapping("/plans")
    @Operation(summary = "Listar planes")
    public ResponseEntity<ApiResponse<PagedResponse<PlanDto>>> getPlans(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getPlans(pageable)));
    }

    @GetMapping("/plans/{id}")
    @Operation(summary = "Obtener plan")
    public ResponseEntity<ApiResponse<PlanDto>> getPlan(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getPlanById(id)));
    }

    @PostMapping("/plans")
    @Operation(summary = "Crear plan")
    public ResponseEntity<ApiResponse<PlanDto>> createPlan(@Valid @RequestBody CreatePlanRequestDto request) {
        PlanDto plan = adminService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(plan, "Plan creado exitosamente"));
    }

    @PutMapping("/plans/{id}")
    @Operation(summary = "Actualizar plan")
    public ResponseEntity<ApiResponse<PlanDto>> updatePlan(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePlanRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updatePlan(id, request), "Plan actualizado"));
    }

    @DeleteMapping("/plans/{id}")
    @Operation(summary = "Eliminar plan (soft-delete)")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable UUID id) {
        adminService.deletePlan(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Plan eliminado"));
    }

    // ========== SYSTEM STATS ==========

    @GetMapping("/stats")
    @Operation(summary = "Estadísticas globales del sistema")
    public ResponseEntity<ApiResponse<SystemStatsDto>> getSystemStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getSystemStats()));
    }
}
