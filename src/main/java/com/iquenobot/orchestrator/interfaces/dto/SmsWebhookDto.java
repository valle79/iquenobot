package com.iquenobot.orchestrator.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsWebhookDto {

    private String messageSid;
    private String from;
    private String to;
    private String body;
    private String numMedia;
    private LocalDateTime timestamp;
    private String accountSid;
}
