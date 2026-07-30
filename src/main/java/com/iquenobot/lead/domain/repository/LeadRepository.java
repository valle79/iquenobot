package com.iquenobot.lead.domain.repository;

import com.iquenobot.lead.domain.entity.Lead;
import com.iquenobot.shared.enums.LeadSource;
import com.iquenobot.shared.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    Optional<Lead> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Page<Lead> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    Page<Lead> findByTenantIdAndStatusAndDeletedFalse(UUID tenantId, LeadStatus status, Pageable pageable);

    Page<Lead> findByTenantIdAndSourceAndDeletedFalse(UUID tenantId, LeadSource source, Pageable pageable);

    Page<Lead> findByTenantIdAndAssignedToIdAndDeletedFalse(UUID tenantId, UUID userId, Pageable pageable);

    Page<Lead> findByTenantIdAndContactIdAndDeletedFalse(UUID tenantId, UUID contactId, Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.deleted = false AND " +
           "(LOWER(l.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.notes) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Lead> searchLeads(@Param("tenantId") UUID tenantId,
                           @Param("search") String search,
                           Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.status IN :statuses AND l.deleted = false")
    Page<Lead> findByTenantIdAndStatusIn(@Param("tenantId") UUID tenantId,
                                         @Param("statuses") List<LeadStatus> statuses,
                                         Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.assignedTo IS NULL " +
           "AND l.status IN ('NEW', 'CONTACTED') AND l.deleted = false")
    Page<Lead> findUnassignedLeads(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.score >= :minScore " +
           "AND l.status NOT IN ('CONVERTED', 'LOST', 'DISQUALIFIED') AND l.deleted = false")
    List<Lead> findHighScoreLeads(@Param("tenantId") UUID tenantId, @Param("minScore") Integer minScore);

    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.deleted = false AND " +
           "l.lastContactAt < :date AND l.status IN ('NEW', 'CONTACTED', 'QUALIFIED')")
    List<Lead> findStaleLeads(@Param("tenantId") UUID tenantId, @Param("date") LocalDateTime date);

    long countByTenantIdAndDeletedFalse(UUID tenantId);

    long countByTenantIdAndStatusAndDeletedFalse(UUID tenantId, LeadStatus status);

    long countByTenantIdAndAssignedToIdAndDeletedFalse(UUID tenantId, UUID userId);

    boolean existsByTenantIdAndContactIdAndCreatedAtAfter(UUID tenantId, UUID contactId, LocalDateTime dateTime);
}
