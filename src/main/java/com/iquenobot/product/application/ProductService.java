package com.iquenobot.product.application;

import com.iquenobot.product.domain.dto.CreateProductRequestDto;
import com.iquenobot.product.domain.dto.ProductDto;
import com.iquenobot.product.domain.entity.Category;
import com.iquenobot.product.domain.entity.Product;
import com.iquenobot.product.domain.repository.CategoryRepository;
import com.iquenobot.product.domain.repository.ProductRepository;
import com.iquenobot.product.interfaces.mapper.ProductMapper;
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.enums.ProductStatus;
import com.iquenobot.shared.exception.BusinessException;
import com.iquenobot.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public ProductDto getById(UUID id) {
        UUID tenantId = getTenantId();
        Product product = productRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        
        return productMapper.toDto(product);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductDto> getAll(Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Product> page = productRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductDto> searchProducts(String search, Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Product> page = productRepository.searchProducts(tenantId, search, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getFeaturedProducts() {
        UUID tenantId = getTenantId();
        List<Product> products = productRepository.findFeaturedProducts(tenantId);
        
        return products.stream().map(productMapper::toDto).toList();
    }

    @Transactional
    public ProductDto create(CreateProductRequestDto request) {
        UUID tenantId = getTenantId();

        // Validate SKU uniqueness
        if (request.getSku() != null && 
            productRepository.existsBySkuAndTenantIdAndDeletedFalse(request.getSku(), tenantId)) {
            throw new BusinessException("Ya existe un producto con ese SKU");
        }

        Product product = productMapper.toEntity(request);
        product.setId(UUID.randomUUID());
        product.setTenantId(tenantId);

        // Set category if provided
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findByIdAndTenantIdAndDeletedFalse(
                    request.getCategoryId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
            product.setCategory(category);
        }

        product = productRepository.save(product);
        
        log.info("Product created: {} for tenant: {}", product.getId(), tenantId);
        
        return productMapper.toDto(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getLowStockProducts() {
        UUID tenantId = getTenantId();
        List<Product> products = productRepository.findLowStockProducts(tenantId);
        
        return products.stream().map(productMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductDto> getByStatus(ProductStatus status, Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Product> page = productRepository.findByTenantIdAndStatusAndDeletedFalse(tenantId, status, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductDto> getByCategory(UUID categoryId, Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Product> page = productRepository.findByTenantIdAndCategoryIdAndDeletedFalse(tenantId, categoryId, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional
    public ProductDto update(UUID id, CreateProductRequestDto request) {
        UUID tenantId = getTenantId();
        Product product = productRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        // Validate SKU uniqueness (excluding current product)
        if (request.getSku() != null && !request.getSku().equals(product.getSku())) {
            if (productRepository.existsBySkuAndTenantIdAndDeletedFalse(request.getSku(), tenantId)) {
                throw new BusinessException("Ya existe un producto con ese SKU");
            }
        }

        // Update fields
        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setShortDescription(request.getShortDescription());
        product.setPrice(request.getPrice());
        product.setCompareAtPrice(request.getCompareAtPrice());
        product.setCostPrice(request.getCostPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setLowStockThreshold(request.getLowStockThreshold());
        product.setStatus(request.getStatus());
        product.setImageUrl(request.getImageUrl());
        product.setImages(request.getImages());
        product.setWeight(request.getWeight());
        product.setWidth(request.getWidth());
        product.setHeight(request.getHeight());
        product.setLength(request.getLength());
        product.setFeatured(request.isFeatured());
        product.setTags(request.getTags());

        // Update category if provided
        if (request.getCategoryId() != null && !request.getCategoryId().equals(
                product.getCategory() != null ? product.getCategory().getId() : null)) {
            Category category = categoryRepository.findByIdAndTenantIdAndDeletedFalse(
                    request.getCategoryId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
            product.setCategory(category);
        }

        product = productRepository.save(product);
        
        log.info("Product updated: {} for tenant: {}", id, tenantId);
        
        return productMapper.toDto(product);
    }

    @Transactional
    public ProductDto increaseStock(UUID id, Integer quantity) {
        UUID tenantId = getTenantId();
        Product product = productRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        if (quantity <= 0) {
            throw new BusinessException("La cantidad debe ser mayor a 0");
        }

        product.increaseStock(quantity);
        product = productRepository.save(product);
        
        log.info("Stock increased for product: {} by {} units", id, quantity);
        
        return productMapper.toDto(product);
    }

    @Transactional
    public ProductDto decreaseStock(UUID id, Integer quantity) {
        UUID tenantId = getTenantId();
        Product product = productRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        if (quantity <= 0) {
            throw new BusinessException("La cantidad debe ser mayor a 0");
        }

        product.decreaseStock(quantity);
        product = productRepository.save(product);
        
        log.info("Stock decreased for product: {} by {} units", id, quantity);
        
        return productMapper.toDto(product);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = getTenantId();
        Product product = productRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        UUID userId = UUID.fromString(TenantContext.getUserId());
        product.softDelete(userId);
        productRepository.save(product);
        
        log.info("Product soft deleted: {} by user: {}", id, userId);
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }

    private PagedResponse<ProductDto> buildPagedResponse(Page<Product> page) {
        return PagedResponse.<ProductDto>builder()
                .content(page.getContent().stream().map(productMapper::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}