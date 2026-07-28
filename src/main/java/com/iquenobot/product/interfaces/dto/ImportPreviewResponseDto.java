package com.iquenobot.product.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportPreviewResponseDto {
    private String fileName;
    private int totalRows;
    private List<String> detectedColumns;
    private List<Map<String, String>> sampleRows;
    private List<FieldOption> productFields;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldOption {
        private String value;
        private String label;
        private boolean required;
    }
}
