package com.iquenobot.product.application;

import com.iquenobot.product.domain.entity.Category;
import com.iquenobot.product.domain.entity.Product;
import com.iquenobot.product.domain.repository.CategoryRepository;
import com.iquenobot.product.domain.repository.ProductRepository;
import com.iquenobot.product.interfaces.dto.*;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.enums.ProductStatus;
import com.iquenobot.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImportService {

    private static final Set<String> CSV_EXTENSIONS = Set.of("csv");
    private static final Set<String> EXCEL_EXTENSIONS = Set.of("xlsx", "xls");

    private static final List<ImportPreviewResponseDto.FieldOption> PRODUCT_FIELDS = List.of(
            new ImportPreviewResponseDto.FieldOption("name", "Nombre", true),
            new ImportPreviewResponseDto.FieldOption("sku", "SKU", false),
            new ImportPreviewResponseDto.FieldOption("price", "Precio", true),
            new ImportPreviewResponseDto.FieldOption("stock_quantity", "Stock", true),
            new ImportPreviewResponseDto.FieldOption("description", "Descripción", false),
            new ImportPreviewResponseDto.FieldOption("short_description", "Descripción corta", false),
            new ImportPreviewResponseDto.FieldOption("category_name", "Categoría", false),
            new ImportPreviewResponseDto.FieldOption("status", "Estado", false),
            new ImportPreviewResponseDto.FieldOption("tags", "Etiquetas", false),
            new ImportPreviewResponseDto.FieldOption("weight", "Peso (kg)", false),
            new ImportPreviewResponseDto.FieldOption("width", "Ancho (cm)", false),
            new ImportPreviewResponseDto.FieldOption("height", "Alto (cm)", false),
            new ImportPreviewResponseDto.FieldOption("length", "Largo (cm)", false),
            new ImportPreviewResponseDto.FieldOption("image_url", "URL imagen", false)
    );

    private static final Map<String, String> COMMON_ALIASES = new HashMap<>();

    static {
        COMMON_ALIASES.put("nombre", "name");
        COMMON_ALIASES.put("producto", "name");
        COMMON_ALIASES.put("product", "name");
        COMMON_ALIASES.put("precio", "price");
        COMMON_ALIASES.put("precio_unitario", "price");
        COMMON_ALIASES.put("unit_price", "price");
        COMMON_ALIASES.put("stock", "stock_quantity");
        COMMON_ALIASES.put("cantidad", "stock_quantity");
        COMMON_ALIASES.put("quantity", "stock_quantity");
        COMMON_ALIASES.put("categoría", "category_name");
        COMMON_ALIASES.put("categoria", "category_name");
        COMMON_ALIASES.put("category", "category_name");
        COMMON_ALIASES.put("descripcion", "description");
        COMMON_ALIASES.put("descripción", "description");
        COMMON_ALIASES.put("desc", "description");
        COMMON_ALIASES.put("etiquetas", "tags");
        COMMON_ALIASES.put("tags", "tags");
        COMMON_ALIASES.put("estado", "status");
        COMMON_ALIASES.put("status", "status");
        COMMON_ALIASES.put("peso", "weight");
        COMMON_ALIASES.put("weight", "weight");
        COMMON_ALIASES.put("ancho", "width");
        COMMON_ALIASES.put("width", "width");
        COMMON_ALIASES.put("alto", "height");
        COMMON_ALIASES.put("height", "height");
        COMMON_ALIASES.put("largo", "length");
        COMMON_ALIASES.put("length", "length");
        COMMON_ALIASES.put("sku", "sku");
        COMMON_ALIASES.put("imagen", "image_url");
        COMMON_ALIASES.put("imagen_url", "image_url");
        COMMON_ALIASES.put("image", "image_url");
        COMMON_ALIASES.put("image_url", "image_url");
    }

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ImportPreviewResponseDto preview(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException("Nombre de archivo inválido");
        }

        String ext = getExtension(originalName).toLowerCase();

        if (CSV_EXTENSIONS.contains(ext)) {
            return previewCsv(file, originalName);
        } else if (EXCEL_EXTENSIONS.contains(ext)) {
            return previewExcel(file, originalName);
        } else {
            throw new BusinessException("Formato no soportado. Usa CSV o Excel (.xlsx, .xls)");
        }
    }

    @Transactional
    public ImportResultResponseDto execute(MultipartFile file, List<ColumnMappingDto> mapping) {
        UUID tenantId = getTenantId();
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new BusinessException("Nombre de archivo inválido");
        }

        String ext = getExtension(originalName).toLowerCase();
        List<String[]> allRows;

        if (CSV_EXTENSIONS.contains(ext)) {
            allRows = parseCsv(file);
        } else if (EXCEL_EXTENSIONS.contains(ext)) {
            allRows = parseExcel(file);
        } else {
            throw new BusinessException("Formato no soportado");
        }

        if (allRows.isEmpty()) {
            throw new BusinessException("El archivo está vacío");
        }

        String[] headers = allRows.get(0);
        // Build column index for mapping: fileColumn -> index in headers
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            headerIndex.put(headers[i].trim().toLowerCase(), i);
        }

        // Build mapping: productField -> index in headers
        Map<String, Integer> fieldToIndex = new HashMap<>();
        for (ColumnMappingDto cm : mapping) {
            Integer idx = headerIndex.get(cm.getFileColumn().trim().toLowerCase());
            if (idx != null) {
                fieldToIndex.put(cm.getProductField(), idx);
            }
        }

        // Pre-fetch categories
        List<Category> allCategories = categoryRepository
                .findByTenantIdAndActiveAndDeletedFalseOrderByDisplayOrderAsc(tenantId, true);
        Map<String, Category> categoryByName = new HashMap<>();
        for (Category c : allCategories) {
            categoryByName.put(c.getName().toLowerCase(), c);
        }

        ImportResultResponseDto.ImportResultResponseDtoBuilder result = ImportResultResponseDto.builder();
        List<ImportResultResponseDto.RowError> errors = new ArrayList<>();
        List<Product> toSave = new ArrayList<>();
        int totalRows = 0;

        for (int rowIdx = 1; rowIdx < allRows.size(); rowIdx++) {
            String[] values = allRows.get(rowIdx);
            if (values.length == 0 || (values.length == 1 && values[0].trim().isEmpty())) {
                continue;
            }

            totalRows++;

            String name = getMappedValue(values, fieldToIndex, "name", headers.length);
            if (name == null || name.isBlank()) {
                errors.add(buildError(rowIdx + 1, "(sin nombre)", "El nombre es obligatorio"));
                continue;
            }

            try {
                String priceStr = getMappedValue(values, fieldToIndex, "price", headers.length);
                BigDecimal price = BigDecimal.ZERO;
                if (priceStr != null && !priceStr.isBlank()) {
                    price = new BigDecimal(priceStr.replaceAll("[^\\d.]", ""));
                }

                Integer stock = 0;
                String stockStr = getMappedValue(values, fieldToIndex, "stock_quantity", headers.length);
                if (stockStr != null && !stockStr.isBlank()) {
                    stock = Integer.parseInt(stockStr.replaceAll("[^\\d]", ""));
                }

                ProductStatus status = ProductStatus.ACTIVE;
                String statusStr = getMappedValue(values, fieldToIndex, "status", headers.length);
                if (statusStr != null && !statusStr.isBlank()) {
                    try {
                        status = ProductStatus.valueOf(statusStr.toUpperCase().trim());
                    } catch (IllegalArgumentException ignored) {}
                }

                String categoryName = getMappedValue(values, fieldToIndex, "category_name", headers.length);
                Category category = null;
                if (categoryName != null && !categoryName.isBlank()) {
                    category = categoryByName.get(categoryName.toLowerCase().trim());
                }

                Product product = Product.builder()
                        .id(UUID.randomUUID())
                        .tenantId(tenantId)
                        .name(name.trim())
                        .sku(getMappedValue(values, fieldToIndex, "sku", headers.length))
                        .description(getMappedValue(values, fieldToIndex, "description", headers.length))
                        .shortDescription(getMappedValue(values, fieldToIndex, "short_description", headers.length))
                        .price(price)
                        .stockQuantity(stock)
                        .lowStockThreshold(10)
                        .status(status)
                        .category(category)
                        .tags(getMappedValue(values, fieldToIndex, "tags", headers.length))
                        .imageUrl(getMappedValue(values, fieldToIndex, "image_url", headers.length))
                        .weight(parseDecimal(getMappedValue(values, fieldToIndex, "weight", headers.length)))
                        .width(parseDecimal(getMappedValue(values, fieldToIndex, "width", headers.length)))
                        .height(parseDecimal(getMappedValue(values, fieldToIndex, "height", headers.length)))
                        .length(parseDecimal(getMappedValue(values, fieldToIndex, "length", headers.length)))
                        .featured(false)
                        .createdBy(UUID.fromString(TenantContext.getUserId()))
                        .updatedBy(UUID.fromString(TenantContext.getUserId()))
                        .build();

                toSave.add(product);
            } catch (Exception e) {
                errors.add(buildError(rowIdx + 1, name, "Error al procesar: " + e.getMessage()));
            }
        }

        if (!toSave.isEmpty()) {
            productRepository.saveAll(toSave);
            log.info("Import: {} products saved for tenant: {}", toSave.size(), tenantId);
        }

        return result
                .totalRows(totalRows)
                .created(toSave.size())
                .skipped(totalRows - toSave.size())
                .errors(errors)
                .build();
    }

    // ── Preview implementations ────────────────────────────────────────────────

    private ImportPreviewResponseDto previewCsv(MultipartFile file, String fileName) {
        List<String[]> rows = parseCsv(file);
        return buildPreview(fileName, rows);
    }

    private ImportPreviewResponseDto previewExcel(MultipartFile file, String fileName) {
        List<String[]> rows = parseExcel(file);
        return buildPreview(fileName, rows);
    }

    private ImportPreviewResponseDto buildPreview(String fileName, List<String[]> rows) {
        if (rows.isEmpty()) {
            return ImportPreviewResponseDto.builder()
                    .fileName(fileName)
                    .totalRows(0)
                    .detectedColumns(List.of())
                    .sampleRows(List.of())
                    .productFields(PRODUCT_FIELDS)
                    .build();
        }

        String[] headers = rows.get(0);
        List<String> detectedColumns = Arrays.stream(headers)
                .map(String::trim)
                .collect(Collectors.toList());

        // Auto-suggest mapping
        Map<String, String> autoMap = autoDetectMapping(detectedColumns);

        // Build sample rows (max 5)
        List<Map<String, String>> sampleRows = new ArrayList<>();
        int maxSamples = Math.min(5, rows.size() - 1);
        for (int i = 1; i <= maxSamples; i++) {
            String[] values = rows.get(i);
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.length && j < values.length; j++) {
                row.put(headers[j].trim(), values[j].trim());
            }
            if (!row.isEmpty()) {
                sampleRows.add(row);
            }
        }

        return ImportPreviewResponseDto.builder()
                .fileName(fileName)
                .totalRows(Math.max(0, rows.size() - 1))
                .detectedColumns(detectedColumns)
                .sampleRows(sampleRows)
                .productFields(PRODUCT_FIELDS)
                .build();
    }

    // ── CSV parsing ────────────────────────────────────────────────────────────

    private List<String[]> parseCsv(MultipartFile file) {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                rows.add(parseCsvLine(line));
            }
        } catch (Exception e) {
            throw new BusinessException("Error al leer el archivo CSV: " + e.getMessage());
        }
        return rows;
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
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

    // ── Excel parsing ──────────────────────────────────────────────────────────

    private List<String[]> parseExcel(MultipartFile file) {
        List<String[]> rows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                List<String> values = new ArrayList<>();
                for (int i = 0; i < row.getLastCellNum(); i++) {
                    Cell cell = row.getCell(i);
                    values.add(getCellValue(cell));
                }
                rows.add(values.toArray(new String[0]));
            }
        } catch (Exception e) {
            throw new BusinessException("Error al leer el archivo Excel: " + e.getMessage());
        }
        return rows;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        yield cell.getStringCellValue();
                    } catch (Exception e2) {
                        yield "";
                    }
                }
            }
            default -> "";
        };
    }

    // ── Auto-detection ─────────────────────────────────────────────────────────

    private Map<String, String> autoDetectMapping(List<String> detectedColumns) {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (String col : detectedColumns) {
            String normalized = col.toLowerCase().replaceAll("[^a-záéíóúñü0-9]", "");
            String field = COMMON_ALIASES.get(normalized);
            if (field == null) {
                field = COMMON_ALIASES.get(col.toLowerCase().trim());
            }
            if (field != null) {
                mapping.put(col, field);
            }
        }
        return mapping;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1);
    }

    private String getMappedValue(String[] values, Map<String, Integer> fieldToIndex, String field, int maxIndex) {
        Integer idx = fieldToIndex.get(field);
        if (idx == null || idx >= values.length || idx >= maxIndex) return null;
        String v = values[idx].trim();
        return v.isEmpty() ? null : v;
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null) return null;
        try {
            return new BigDecimal(value.replaceAll("[^\\d.]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ImportResultResponseDto.RowError buildError(int row, String name, String reason) {
        ImportResultResponseDto.RowError err = new ImportResultResponseDto.RowError();
        err.setRow(row);
        err.setProductName(name);
        err.setReason(reason);
        return err;
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }
}
