package com.iquenobot.product.domain.repository;

import com.iquenobot.product.domain.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Page<Category> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    List<Category> findByTenantIdAndActiveAndDeletedFalseOrderByDisplayOrderAsc(UUID tenantId, boolean active);

    @Query("SELECT c FROM Category c WHERE c.tenantId = :tenantId AND c.deleted = false AND " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Category> searchCategories(@Param("tenantId") UUID tenantId,
                                    @Param("search") String search,
                                    Pageable pageable);

    long countByTenantIdAndDeletedFalse(UUID tenantId);

    boolean existsByNameAndTenantIdAndDeletedFalse(String name, UUID tenantId);
}