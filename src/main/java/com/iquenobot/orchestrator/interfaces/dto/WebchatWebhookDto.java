package com.iquenobot.orchestrator.interfaces.dto;

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
public class WebchatWebhookDto {

    private String sessionId;
    private String visitorId;
    private String sourceIdentifier;
    private String sourceName;
    private String message;
    private String type;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
}
