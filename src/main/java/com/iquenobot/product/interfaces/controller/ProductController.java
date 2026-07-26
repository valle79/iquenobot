package com.iquenobot.product.interfaces.controller;

import com.iquenobot.product.application.ProductService;
import com.iquenobot.product.domain.dto.CreateProductRequestDto;
import com.iquenobot.product.domain.dto.ProductDto;
import com.iquenobot.shared.domain.dto.ApiResponse;
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.enums.ProductStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Endpoints para gestión de productos")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<ProductDto>> getById(@PathVariable UUID id) {
        ProductDto product = productService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping
    @Operation(summary = "Obtener todos los productos paginados")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDto>>> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<ProductDto> products = productService.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar productos")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDto>>> search(
            @RequestParam String query,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        PagedResponse<ProductDto> products = productService.searchProducts(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/featured")
    @Operation(summary = "Obtener productos destacados")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getFeatured() {
        List<ProductDto> products = productService.getFeaturedProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Obtener productos con stock bajo")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getLowStock() {
        List<ProductDto> products = productService.getLowStockProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Obtener productos por estado")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDto>>> getByStatus(
            @PathVariable ProductStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<ProductDto> products = productService.getByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Obtener productos por categoría")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDto>>> getByCategory(
            @PathVariable UUID categoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<ProductDto> products = productService.getByCategory(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PostMapping
    @Operation(summary = "Crear producto")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<ProductDto>> create(@Valid @RequestBody CreateProductRequestDto request) {
        ProductDto product = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Producto creado exitosamente"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<ProductDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateProductRequestDto request) {
        ProductDto product = productService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(product, "Producto actualizado exitosamente"));
    }

    @PutMapping("/{id}/stock/increase")
    @Operation(summary = "Aumentar stock")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<ProductDto>> increaseStock(
            @PathVariable UUID id,
            @RequestParam Integer quantity) {
        ProductDto product = productService.increaseStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success(product, "Stock aumentado exitosamente"));
    }

    @PutMapping("/{id}/stock/decrease")
    @Operation(summary = "Disminuir stock")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<ProductDto>> decreaseStock(
            @PathVariable UUID id,
            @RequestParam Integer quantity) {
        ProductDto product = productService.decreaseStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success(product, "Stock disminuido exitosamente"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Producto eliminado exitosamente"));
    }
}
