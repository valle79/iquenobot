package com.iquenobot.ai.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WhatsAppWebhookDto {

    private String event;
    private String instanceId;
    private String messageId;
    private String from;
    private String to;
    private String type;
    private String text;
    private String mediaUrl;
    private String caption;
    private String filename;
    private String mimeType;
    private LocalDateTime timestamp;
    private Map<String, Object> data;
}