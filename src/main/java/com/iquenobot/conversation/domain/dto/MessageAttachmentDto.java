package com.iquenobot.conversation.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.iquenobot.shared.enums.AttachmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageAttachmentDto {

    private UUID id;
    private AttachmentType type;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String formattedFileSize;
    private String mimeType;
    private String thumbnailUrl;
    private Integer durationSeconds;
    private Integer width;
    private Integer height;
    private String caption;
}