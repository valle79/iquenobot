package com.iquenobot.auth.domain.repository;

import com.iquenobot.auth.domain.entity.Tenant;
import com.iquenobot.shared.enums.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySubdomainAndDeletedFalse(String subdomain);

    Optional<Tenant> findByIdAndDeletedFalse(UUID id);

    boolean existsBySubdomainAndDeletedFalse(String subdomain);

    boolean existsByContactEmailAndDeletedFalse(String contactEmail);

    Page<Tenant> findByDeletedFalse(Pageable pageable);

    Page<Tenant> findByStatusAndDeletedFalse(TenantStatus status, Pageable pageable);

    @Query("SELECT t FROM Tenant t WHERE t.deleted = false AND " +
           "(LOWER(t.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.subdomain) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.contactEmail) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Tenant> findBySearchAndDeletedFalse(@Param("search") String search, Pageable pageable);

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(TenantStatus status);

    @Query("SELECT t FROM Tenant t WHERE t.subscriptionExpiresAt <= :date AND t.status = 'ACTIVE' AND t.deleted = false")
    List<Tenant> findTenantsWithExpiredSubscriptions(@Param("date") LocalDate date);

    @Query("SELECT t FROM Tenant t WHERE t.subscriptionExpiresAt BETWEEN :startDate AND :endDate AND t.status = 'ACTIVE' AND t.deleted = false")
    List<Tenant> findTenantsWithExpiringSubscriptions(@Param("startDate") LocalDate startDate, 
                                                       @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.status = 'ACTIVE' AND t.deleted = false")
    long countActiveTenants();

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.status = 'TRIAL' AND t.deleted = false")
    long countTrialTenants();

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.status = 'ACTIVE' AND t.subscriptionExpiresAt < CURRENT_DATE AND t.deleted = false")
    long countExpiredTenants();
}