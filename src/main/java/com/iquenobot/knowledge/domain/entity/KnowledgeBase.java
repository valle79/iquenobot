package com.iquenobot.knowledge.domain.entity;

import com.iquenobot.shared.common.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "knowledge_base")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBase extends SoftDeletableEntity {

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "tags", length = 500)
    private String tags;
}
