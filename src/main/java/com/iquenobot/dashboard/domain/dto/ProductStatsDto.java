package com.iquenobot.dashboard.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatsDto {

    private long totalProducts;
    private long activeProducts;
    private long outOfStockProducts;
    private long lowStockProducts;
    private long featuredProducts;
    private long totalCategories;
}
