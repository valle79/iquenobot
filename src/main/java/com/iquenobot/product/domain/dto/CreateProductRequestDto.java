package com.iquenobot.product.domain.dto;

import com.iquenobot.shared.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequestDto {

    @Size(max = 100, message = "El SKU no puede exceder 100 caracteres")
    private String sku;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 200, message = "El nombre debe tener entre 2 y 200 caracteres")
    private String name;

    @Size(max = 5000, message = "La descripción no puede exceder 5000 caracteres")
    private String description;

    @Size(max = 500, message = "La descripción corta no puede exceder 500 caracteres")
    private String shortDescription;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio debe ser mayor o igual a 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0", message = "El precio de comparación debe ser mayor o igual a 0")
    private BigDecimal compareAtPrice;

    @DecimalMin(value = "0.0", message = "El costo debe ser mayor o igual a 0")
    private BigDecimal costPrice;

    private UUID categoryId;

    @NotNull(message = "La cantidad en stock es obligatoria")
    @Min(value = 0, message = "La cantidad debe ser mayor o igual a 0")
    private Integer stockQuantity;

    @Min(value = 0, message = "El umbral debe ser mayor o igual a 0")
    private Integer lowStockThreshold;

    @NotNull(message = "El estado es obligatorio")
    private ProductStatus status;

    @Size(max = 1000, message = "La URL de imagen no puede exceder 1000 caracteres")
    private String imageUrl;

    private String images;

    private BigDecimal weight;
    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal length;

    private boolean featured;
    private String tags;
}