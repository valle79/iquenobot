package com.iquenobot.product.application;

import com.iquenobot.product.domain.dto.CreateProductRequestDto;
import com.iquenobot.product.domain.dto.ProductDto;
import com.iquenobot.product.domain.dto.ProductImportResultDto;
import com.iquenobot.product.domain.dto.ProductImportResultDto.ImportError;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        product.setLowStockThreshold(request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 10);
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
    public ProductImportResultDto importFromCsv(String csvContent) {
        UUID tenantId = getTenantId();
        ProductImportResultDto result = ProductImportResultDto.builder().build();
        String[] lines = csvContent.split("\n");

        if (lines.length < 2) {
            result.setSkipped(0);
            return result;
        }

        // Parse header
        String[] headers = parseCsvLine(lines[0].trim());
        
        // Build column index map
        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            colIndex.put(headers[i].toLowerCase().replace(" ", "_"), i);
        }

        // Pre-fetch categories by name for matching
        List<Category> allCategories = categoryRepository.findByTenantIdAndActiveAndDeletedFalseOrderByDisplayOrderAsc(tenantId, true);
        Map<String, Category> categoryByName = new HashMap<>();
        for (Category c : allCategories) {
            categoryByName.put(c.getName().toLowerCase(), c);
        }

        List<Product> toSave = new ArrayList<>();
        
        for (int rowIdx = 1; rowIdx < lines.length; rowIdx++) {
            String line = lines[rowIdx].trim();
            if (line.isEmpty()) continue;

            result.setTotalRows(result.getTotalRows() + 1);
            String[] values = parseCsvLine(line);
            String name = getColValue(values, colIndex, "name");

            if (name == null || name.isBlank()) {
                result.getErrors().add(ImportError.builder()
                        .row(rowIdx + 1)
                        .productName("(sin nombre)")
                        .reason("El nombre es obligatorio")
                        .build());
                continue;
            }

            try {
                String priceStr = getColValue(values, colIndex, "price");
                BigDecimal price = BigDecimal.ZERO;
                if (priceStr != null && !priceStr.isBlank()) {
                    price = new BigDecimal(priceStr.replaceAll("[^\\d.]", ""));
                }

                Integer stock = 0;
                String stockStr = getColValue(values, colIndex, "stock_quantity");
                if (stockStr != null && !stockStr.isBlank()) {
                    stock = Integer.parseInt(stockStr.replaceAll("[^\\d]", ""));
                }

                ProductStatus status = ProductStatus.ACTIVE;
                String statusStr = getColValue(values, colIndex, "status");
                if (statusStr != null && !statusStr.isBlank()) {
                    try {
                        status = ProductStatus.valueOf(statusStr.toUpperCase());
                    } catch (IllegalArgumentException ignored) {}
                }

                String categoryName = getColValue(values, colIndex, "category_name");
                Category category = null;
                if (categoryName != null && !categoryName.isBlank()) {
                    category = categoryByName.get(categoryName.toLowerCase());
                }

                BigDecimal weight = parseDecimal(getColValue(values, colIndex, "weight"));
                BigDecimal width = parseDecimal(getColValue(values, colIndex, "width"));
                BigDecimal height = parseDecimal(getColValue(values, colIndex, "height"));
                BigDecimal length = parseDecimal(getColValue(values, colIndex, "length"));

                Product product = Product.builder()
                        .id(UUID.randomUUID())
                        .tenantId(tenantId)
                        .name(name.trim())
                        .sku(getColValue(values, colIndex, "sku"))
                        .description(getColValue(values, colIndex, "description"))
                        .shortDescription(getColValue(values, colIndex, "short_description"))
                        .price(price)
                        .stockQuantity(stock)
                        .lowStockThreshold(10)
                        .status(status)
                        .category(category)
                        .tags(getColValue(values, colIndex, "tags"))
                        .imageUrl(getColValue(values, colIndex, "image_url"))
                        .weight(weight)
                        .width(width)
                        .height(height)
                        .length(length)
                        .featured(false)
                        .createdBy(UUID.fromString(TenantContext.getUserId()))
                        .updatedBy(UUID.fromString(TenantContext.getUserId()))
                        .build();

                toSave.add(product);
                result.setCreated(result.getCreated() + 1);
            } catch (Exception e) {
                result.getErrors().add(ImportError.builder()
                        .row(rowIdx + 1)
                        .productName(name)
                        .reason("Error al procesar: " + e.getMessage())
                        .build());
            }
        }

        if (!toSave.isEmpty()) {
            productRepository.saveAll(toSave);
            log.info("Bulk import: {} products saved for tenant: {}", toSave.size(), tenantId);
        }

        return result;
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value.replaceAll("[^\\d.]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getColValue(String[] values, java.util.Map<String, Integer> colIndex, String colName) {
        Integer idx = colIndex.get(colName);
        if (idx == null || idx >= values.length) return null;
        return values[idx].trim();
    }

    private String[] parseCsvLine(String line) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

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