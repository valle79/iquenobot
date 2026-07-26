package com.iquenobot.orchestrator.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailWebhookDto {

    private String messageId;
    private String from;
    private String fromName;
    private String to;
    private String subject;
    private String bodyText;
    private String bodyHtml;
    private LocalDateTime receivedAt;
    private List<Attachment> attachments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attachment {
        private String filename;
        private String contentType;
        private Long size;
        private String url;
    }
}
