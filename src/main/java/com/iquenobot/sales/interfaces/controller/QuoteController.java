package com.iquenobot.sales.interfaces.controller;

import com.iquenobot.sales.application.QuoteManagementService;
import com.iquenobot.sales.domain.dto.QuoteDetailDto;
import com.iquenobot.sales.domain.dto.QuoteHistoryDto;
import com.iquenobot.sales.domain.dto.QuoteResendResultDto;
import com.iquenobot.sales.domain.dto.QuoteSummaryDto;
import com.iquenobot.sales.domain.model.QuoteSearchCriteria;
import com.iquenobot.shared.domain.dto.ApiResponse;
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.enums.QuoteStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cotizaciones", description = "Gestión documental de cotizaciones del CRM")
public class QuoteController {

    private final QuoteManagementService quoteManagementService;

    @GetMapping
    @Operation(summary = "Listar cotizaciones", description = "Lista paginada de cotizaciones con filtros por cliente, número, teléfono, fechas, estado, generador y montos")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<QuoteSummaryDto>>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String quoteNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(required = false) QuoteStatus status,
            @RequestParam(required = false) String generatedBy,
            @RequestParam(required = false) BigDecimal minTotal,
            @RequestParam(required = false) BigDecimal maxTotal,
            @PageableDefault(size = 20) Pageable pageable) {

        boolean botGenerated = "BOT".equalsIgnoreCase(generatedBy);
        UUID generatedByUuid = (!botGenerated && generatedBy != null && !generatedBy.isBlank())
                ? UUID.fromString(generatedBy)
                : null;

        QuoteSearchCriteria criteria = new QuoteSearchCriteria(
                query, customer, quoteNumber, phone,
                dateFrom, dateTo, status, generatedByUuid, botGenerated, minTotal, maxTotal);

        PagedResponse<QuoteSummaryDto> result = quoteManagementService.search(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de cotización", description = "Detalle completo con ítems e historial")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<QuoteDetailDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(quoteManagementService.getDetail(id)));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Descargar PDF", description = "Descarga el PDF de la cotización (autenticado y por tenant)")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        QuoteManagementService.DownloadedPdf pdf = quoteManagementService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(pdf.filename(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(pdf.bytes());
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Historial de cotización", description = "Auditoría completa de acciones de la cotización")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<List<QuoteHistoryDto>>> getHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(quoteManagementService.getHistory(id)));
    }

    @PostMapping("/{id}/resend")
    @Operation(summary = "Reenviar PDF al cliente", description = "Reenvía el PDF almacenado al WhatsApp del cliente sin regenerarlo")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<QuoteResendResultDto>> resend(@PathVariable UUID id) {
        QuoteResendResultDto result = quoteManagementService.resend(id);
        return ResponseEntity.ok(ApiResponse.success(result, result.message()));
    }

    @PostMapping("/{id}/resend-to")
    @Operation(summary = "Reenviar PDF a otro número", description = "Reenvía el PDF almacenado a un número de WhatsApp indicado")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<QuoteResendResultDto>> resendTo(
            @PathVariable UUID id,
            @Valid @RequestBody ResendToRequest request) {
        QuoteResendResultDto result = quoteManagementService.resendTo(id, request.phone());
        return ResponseEntity.ok(ApiResponse.success(result, result.message()));
    }

    @PostMapping("/{id}/regenerate")
    @Operation(summary = "Regenerar PDF", description = "Regenera el PDF con los datos actuales y reemplaza el almacenado")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<QuoteDetailDto>> regenerate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(quoteManagementService.regenerate(id),
                "PDF regenerado exitosamente"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Anular cotización", description = "Anula la cotización (baja lógica)")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable UUID id) {
        quoteManagementService.cancel(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Cotización anulada exitosamente"));
    }

    public record ResendToRequest(
            @NotBlank(message = "El número de WhatsApp es obligatorio") String phone) {}
}
