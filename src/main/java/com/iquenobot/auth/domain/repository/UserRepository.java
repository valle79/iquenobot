package com.iquenobot.auth.domain.repository;

import com.iquenobot.auth.domain.entity.User;
import com.iquenobot.shared.enums.RoleType;
import com.iquenobot.shared.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndTenantIdAndDeletedFalse(String email, UUID tenantId);

    Optional<User> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Optional<User> findByEmailVerificationTokenAndDeletedFalse(String token);

    Optional<User> findByPasswordResetTokenAndDeletedFalse(String token);

    boolean existsByEmailAndTenantIdAndDeletedFalse(String email, UUID tenantId);

    Page<User> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    Page<User> findByTenantIdAndRoleAndDeletedFalse(UUID tenantId, RoleType role, Pageable pageable);

    Page<User> findByTenantIdAndStatusAndDeletedFalse(UUID tenantId, UserStatus status, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.deleted = false AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findByTenantIdAndSearchAndDeletedFalse(@Param("tenantId") UUID tenantId,
                                                      @Param("search") String search,
                                                      Pageable pageable);

    long countByTenantIdAndDeletedFalse(UUID tenantId);

    long countByTenantIdAndStatusAndDeletedFalse(UUID tenantId, UserStatus status);

    long countByTenantIdAndRoleAndDeletedFalse(UUID tenantId, RoleType role);

    List<User> findByTenantIdAndStatusAndLastLoginAtBeforeAndDeletedFalse(
            UUID tenantId, UserStatus status, LocalDateTime lastLoginBefore);

    @Modifying
    @Query("UPDATE User u SET u.loginAttempts = 0, u.lockedUntil = null WHERE u.lockedUntil < :now")
    int unlockExpiredAccounts(@Param("now") LocalDateTime now);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.role IN :roles AND u.status = 'ACTIVE' AND u.deleted = false")
    List<User> findActiveUsersByRoles(@Param("tenantId") UUID tenantId, @Param("roles") List<RoleType> roles);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.status = 'ACTIVE' AND u.deleted = false")
    long countActiveUsers(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.deleted = false")
    long countAllActive();
}