package com.iquenobot.shared.interfaces.controller;

import com.iquenobot.shared.application.FileUploadService;
import com.iquenobot.shared.domain.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Archivos", description = "Carga de archivos (imágenes, documentos, adjuntos)")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir imagen", description = "Sube una imagen (JPEG, PNG, WebP, GIF). Máximo 5MB.")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = fileUploadService.uploadImage(file);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url), "Imagen subida exitosamente"));
    }

    @PostMapping(value = "/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir documento", description = "Sube un documento (PDF, DOC, TXT, CSV). Máximo 10MB.")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadDocument(@RequestParam("file") MultipartFile file) {
        String url = fileUploadService.uploadDocument(file);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url), "Documento subido exitosamente"));
    }

    @PostMapping(value = "/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir adjunto", description = "Sube un archivo adjunto. Máximo 10MB.")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAttachment(@RequestParam("file") MultipartFile file) {
        String url = fileUploadService.uploadAttachment(file);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url), "Archivo subido exitosamente"));
    }
}
