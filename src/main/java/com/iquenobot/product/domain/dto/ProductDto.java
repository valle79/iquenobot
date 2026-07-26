package com.iquenobot.product.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.iquenobot.shared.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDto {

    private UUID id;
    private String sku;
    private String name;
    private String description;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private BigDecimal costPrice;
    private CategoryDto category;
    private Integer stockQuantity;
    private Integer lowStockThreshold;
    private ProductStatus status;
    private String imageUrl;
    private String images;
    private BigDecimal weight;
    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal length;
    private boolean featured;
    private String tags;
    private boolean available;
    private boolean lowStock;
    private boolean hasDiscount;
    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}