package com.iquenobot.product.domain.repository;

import com.iquenobot.product.domain.entity.Product;
import com.iquenobot.shared.enums.ProductStatus;
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
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Optional<Product> findBySkuAndTenantIdAndDeletedFalse(String sku, UUID tenantId);

    Page<Product> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    Page<Product> findByTenantIdAndStatusAndDeletedFalse(UUID tenantId, ProductStatus status, Pageable pageable);

    Page<Product> findByTenantIdAndCategoryIdAndDeletedFalse(UUID tenantId, UUID categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND p.deleted = false AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchProducts(@Param("tenantId") UUID tenantId,
                                 @Param("search") String search,
                                 Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND p.status = 'ACTIVE' " +
           "AND p.stockQuantity > 0 AND p.deleted = false")
    Page<Product> findAvailableProducts(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND p.featured = true " +
           "AND p.status = 'ACTIVE' AND p.deleted = false")
    List<Product> findFeaturedProducts(@Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND p.stockQuantity <= p.lowStockThreshold " +
           "AND p.status = 'ACTIVE' AND p.deleted = false")
    List<Product> findLowStockProducts(@Param("tenantId") UUID tenantId);

    long countByTenantIdAndDeletedFalse(UUID tenantId);

    long countByTenantIdAndStatusAndDeletedFalse(UUID tenantId, ProductStatus status);

    boolean existsBySkuAndTenantIdAndDeletedFalse(String sku, UUID tenantId);
}