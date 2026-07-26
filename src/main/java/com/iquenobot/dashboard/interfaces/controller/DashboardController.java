package com.iquenobot.dashboard.interfaces.controller;

import com.iquenobot.dashboard.application.DashboardService;
import com.iquenobot.dashboard.domain.dto.ContactStatsDto;
import com.iquenobot.dashboard.domain.dto.ConversationStatsDto;
import com.iquenobot.dashboard.domain.dto.DashboardOverviewDto;
import com.iquenobot.dashboard.domain.dto.LeadStatsDto;
import com.iquenobot.dashboard.domain.dto.ProductStatsDto;
import com.iquenobot.dashboard.domain.dto.UserActivityDto;
import com.iquenobot.shared.domain.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Endpoints para estadísticas y métricas del dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @Operation(summary = "Obtener resumen general del dashboard")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<DashboardOverviewDto>> getOverview() {
        DashboardOverviewDto overview = dashboardService.getOverview();
        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    @GetMapping("/conversations/stats")
    @Operation(summary = "Obtener estadísticas de conversaciones")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<ConversationStatsDto>> getConversationStats() {
        ConversationStatsDto stats = dashboardService.getConversationStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/leads/stats")
    @Operation(summary = "Obtener estadísticas de leads")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<LeadStatsDto>> getLeadStats() {
        LeadStatsDto stats = dashboardService.getLeadStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/contacts/stats")
    @Operation(summary = "Obtener estadísticas de contactos")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<ContactStatsDto>> getContactStats() {
        ContactStatsDto stats = dashboardService.getContactStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/products/stats")
    @Operation(summary = "Obtener estadísticas de productos")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<ProductStatsDto>> getProductStats() {
        ProductStatsDto stats = dashboardService.getProductStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/users/activity")
    @Operation(summary = "Obtener actividad de usuarios")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserActivityDto>>> getUserActivity() {
        List<UserActivityDto> activity = dashboardService.getUserActivity();
        return ResponseEntity.ok(ApiResponse.success(activity));
    }
}
