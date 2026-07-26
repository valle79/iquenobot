package com.iquenobot.orchestrator.interfaces.controller;

import com.iquenobot.orchestrator.domain.entity.AuditLog;
import com.iquenobot.orchestrator.domain.repository.AuditLogRepository;
import com.iquenobot.shared.domain.dto.ApiResponse;
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for querying orchestrator audit logs.
 * Provides compliance and debugging capabilities.
 */
@RestController
@RequestMapping("/api/v1/orchestrator/audit")
@RequiredArgsConstructor
@Tag(name = "Orchestrator Audit", description = "Audit log endpoints for compliance and debugging")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @Operation(summary = "Get all audit logs for tenant")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLog>>> getAll(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        UUID tenantId = getTenantId();
        Page<AuditLog> page = auditLogRepository.findByTenantId(tenantId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(buildPagedResponse(page)));
    }

    @GetMapping("/conversation/{conversationId}")
    @Operation(summary = "Get audit logs for specific conversation")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLog>>> getByConversation(
            @PathVariable String conversationId,
            @PageableDefault(size = 100, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        
        UUID tenantId = getTenantId();
        Page<AuditLog> page = auditLogRepository.findByTenantIdAndConversationId(
                tenantId, conversationId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(buildPagedResponse(page)));
    }

    @GetMapping("/channel/{channel}")
    @Operation(summary = "Get audit logs by channel")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLog>>> getByChannel(
            @PathVariable String channel,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        UUID tenantId = getTenantId();
        Page<AuditLog> page = auditLogRepository.findByTenantIdAndChannel(tenantId, channel, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(buildPagedResponse(page)));
    }

    @GetMapping("/event-type/{eventType}")
    @Operation(summary = "Get audit logs by event type")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLog>>> getByEventType(
            @PathVariable String eventType,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        UUID tenantId = getTenantId();
        Page<AuditLog> page = auditLogRepository.findByTenantIdAndEventType(tenantId, eventType, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(buildPagedResponse(page)));
    }

    @GetMapping("/failed")
    @Operation(summary = "Get failed operations")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLog>>> getFailedOperations(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        UUID tenantId = getTenantId();
        Page<AuditLog> page = auditLogRepository.findFailedOperations(tenantId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(buildPagedResponse(page)));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get audit statistics")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        UUID tenantId = getTenantId();
        
        Map<String, Object> stats = new HashMap<>();
        
        // Total operations
        stats.put("totalOperations", auditLogRepository.countByTenantId(tenantId));
        
        // Success vs Failed
        stats.put("successfulOperations", auditLogRepository.countByTenantIdAndStatus(tenantId, "SUCCESS"));
        stats.put("failedOperations", auditLogRepository.countByTenantIdAndStatus(tenantId, "FAILED"));
        
        // By channel
        List<Object[]> channelStats = auditLogRepository.countByChannel(tenantId);
        Map<String, Long> byChannel = new HashMap<>();
        for (Object[] row : channelStats) {
            byChannel.put((String) row[0], (Long) row[1]);
        }
        stats.put("byChannel", byChannel);
        
        // By action type
        List<Object[]> actionStats = auditLogRepository.countByActionType(tenantId);
        Map<String, Long> byAction = new HashMap<>();
        for (Object[] row : actionStats) {
            if (row[0] != null) {
                byAction.put((String) row[0], (Long) row[1]);
            }
        }
        stats.put("byActionType", byAction);
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }

    private PagedResponse<AuditLog> buildPagedResponse(Page<AuditLog> page) {
        return PagedResponse.<AuditLog>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
