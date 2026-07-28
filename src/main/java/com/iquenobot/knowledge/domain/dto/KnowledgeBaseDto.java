package com.iquenobot.knowledge.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseDto {
    private UUID id;
    private String title;
    private String content;
    private String sourceType;
    private String sourceUrl;
    private String fileUrl;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
