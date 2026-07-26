package com.iquenobot.product.interfaces.mapper;

import com.iquenobot.product.domain.dto.CategoryDto;
import com.iquenobot.product.domain.dto.CreateCategoryRequestDto;
import com.iquenobot.product.domain.dto.CreateProductRequestDto;
import com.iquenobot.product.domain.dto.ProductDto;
import com.iquenobot.product.domain.entity.Category;
import com.iquenobot.product.domain.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMapper {

    @Mapping(target = "available", expression = "java(product.isAvailable())")
    @Mapping(target = "lowStock", expression = "java(product.isLowStock())")
    @Mapping(target = "hasDiscount", expression = "java(product.hasDiscount())")
    @Mapping(target = "discountAmount", expression = "java(product.getDiscountAmount())")
    @Mapping(target = "discountPercentage", expression = "java(product.getDiscountPercentage())")
    ProductDto toDto(Product product);

    @Mapping(target = "productCount", expression = "java(category.getProductCount())")
    CategoryDto toCategoryDto(Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Product toEntity(CreateProductRequestDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Category toCategoryEntity(CreateCategoryRequestDto dto);
}