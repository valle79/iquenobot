package com.iquenobot.product.interfaces.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iquenobot.product.application.ProductImportService;
import com.iquenobot.product.interfaces.dto.ColumnMappingDto;
import com.iquenobot.product.interfaces.dto.ImportExecuteRequestDto;
import com.iquenobot.product.interfaces.dto.ImportPreviewResponseDto;
import com.iquenobot.product.interfaces.dto.ImportResultResponseDto;
import com.iquenobot.shared.domain.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products/import")
@RequiredArgsConstructor
@Tag(name = "Importación de Productos")
@SecurityRequirement(name = "bearerAuth")
public class ProductImportController {

    private final ProductImportService productImportService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Previsualizar importación", description = "Sube un CSV/Excel para detectar columnas y ver una muestra")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<ImportPreviewResponseDto>> preview(
            @RequestParam("file") MultipartFile file) {
        ImportPreviewResponseDto preview = productImportService.preview(file);
        return ResponseEntity.ok(ApiResponse.success(preview));
    }

    @PostMapping(value = "/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Ejecutar importación", description = "Importa productos desde CSV/Excel con mapeo de columnas")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<ImportResultResponseDto>> execute(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mapping") String mappingJson) {
        try {
            ImportExecuteRequestDto request = objectMapper.readValue(mappingJson, ImportExecuteRequestDto.class);
            ImportResultResponseDto result = productImportService.execute(file, request.getColumnMapping());
            return ResponseEntity.ok(ApiResponse.success(result, "Importación completada"));
        } catch (Exception e) {
            throw new RuntimeException("Error al importar: " + e.getMessage());
        }
    }
}
