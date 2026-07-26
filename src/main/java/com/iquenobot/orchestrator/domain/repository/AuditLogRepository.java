package com.iquenobot.orchestrator.domain.repository;

import com.iquenobot.orchestrator.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByTenantId(UUID tenantId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndConversationId(UUID tenantId, String conversationId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndChannel(UUID tenantId, String channel, Pageable pageable);

    Page<AuditLog> findByTenantIdAndEventType(UUID tenantId, String eventType, Pageable pageable);

    Page<AuditLog> findByTenantIdAndStatus(UUID tenantId, String status, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.createdAt BETWEEN :startDate AND :endDate")
    List<AuditLog> findByTenantIdAndDateRange(@Param("tenantId") UUID tenantId,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.status = 'FAILED' " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findFailedOperations(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a.channel, COUNT(a) FROM AuditLog a WHERE a.tenantId = :tenantId " +
           "GROUP BY a.channel")
    List<Object[]> countByChannel(@Param("tenantId") UUID tenantId);

    @Query("SELECT a.actionType, COUNT(a) FROM AuditLog a WHERE a.tenantId = :tenantId " +
           "GROUP BY a.actionType")
    List<Object[]> countByActionType(@Param("tenantId") UUID tenantId);

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, String status);
}
