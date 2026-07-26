package com.iquenobot.role.domain.repository;

import com.iquenobot.role.domain.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Optional<Role> findByNameAndTenantIdAndDeletedFalse(String name, UUID tenantId);

    Page<Role> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    boolean existsByNameAndTenantIdAndDeletedFalse(String name, UUID tenantId);

    long countByTenantIdAndDeletedFalse(UUID tenantId);
}
