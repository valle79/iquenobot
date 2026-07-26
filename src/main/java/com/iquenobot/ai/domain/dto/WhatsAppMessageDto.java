package com.iquenobot.ai.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.iquenobot.shared.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WhatsAppMessageDto {

    private String from;
    private String to;
    private MessageType type;
    private String text;
    private String mediaUrl;
    private String caption;
    private String filename;
    private String mimeType;
    private Map<String, Object> metadata;
    private String replyToMessageId;
}