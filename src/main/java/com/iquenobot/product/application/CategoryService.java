package com.iquenobot.product.application;

import com.iquenobot.product.domain.dto.CategoryDto;
import com.iquenobot.product.domain.dto.CreateCategoryRequestDto;
import com.iquenobot.product.domain.entity.Category;
import com.iquenobot.product.domain.repository.CategoryRepository;
import com.iquenobot.product.interfaces.mapper.ProductMapper;
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.domain.util.TenantContext;
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
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public CategoryDto getById(UUID id) {
        UUID tenantId = getTenantId();
        Category category = categoryRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        
        return productMapper.toCategoryDto(category);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CategoryDto> getAll(Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Category> page = categoryRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getActiveCategories() {
        UUID tenantId = getTenantId();
        List<Category> categories = categoryRepository.findByTenantIdAndActiveAndDeletedFalseOrderByDisplayOrderAsc(
                tenantId, true);
        
        return categories.stream().map(productMapper::toCategoryDto).toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<CategoryDto> searchCategories(String search, Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Category> page = categoryRepository.searchCategories(tenantId, search, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional
    public CategoryDto create(CreateCategoryRequestDto request) {
        UUID tenantId = getTenantId();

        // Validate name uniqueness
        if (categoryRepository.existsByNameAndTenantIdAndDeletedFalse(request.getName(), tenantId)) {
            throw new BusinessException("Ya existe una categoría con ese nombre");
        }

        Category category = productMapper.toCategoryEntity(request);
        category.setId(UUID.randomUUID());
        category.setTenantId(tenantId);

        category = categoryRepository.save(category);
        
        log.info("Category created: {} for tenant: {}", category.getId(), tenantId);
        
        return productMapper.toCategoryDto(category);
    }

    @Transactional
    public CategoryDto update(UUID id, CreateCategoryRequestDto request) {
        UUID tenantId = getTenantId();
        Category category = categoryRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        // Validate name uniqueness (excluding current category)
        if (!request.getName().equals(category.getName()) &&
            categoryRepository.existsByNameAndTenantIdAndDeletedFalse(request.getName(), tenantId)) {
            throw new BusinessException("Ya existe una categoría con ese nombre");
        }

        // Update fields
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIconUrl(request.getIconUrl());
        category.setDisplayOrder(request.getDisplayOrder());
        category.setActive(request.isActive());

        category = categoryRepository.save(category);
        
        log.info("Category updated: {} for tenant: {}", id, tenantId);
        
        return productMapper.toCategoryDto(category);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = getTenantId();
        Category category = categoryRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        UUID userId = UUID.fromString(TenantContext.getUserId());
        category.softDelete(userId);
        categoryRepository.save(category);
        
        log.info("Category soft deleted: {} by user: {}", id, userId);
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }

    private PagedResponse<CategoryDto> buildPagedResponse(Page<Category> page) {
        return PagedResponse.<CategoryDto>builder()
                .content(page.getContent().stream().map(productMapper::toCategoryDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
