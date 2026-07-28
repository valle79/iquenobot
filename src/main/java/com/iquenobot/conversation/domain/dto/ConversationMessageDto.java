package com.iquenobot.conversation.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.iquenobot.shared.enums.MessageDirection;
import com.iquenobot.shared.enums.MessageType;
import com.iquenobot.shared.enums.MessageStatus;
import com.iquenobot.shared.enums.SenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationMessageDto {

    private UUID id;
    private UUID conversationId;
    private UUID userId;
    private String userName;
    private MessageDirection direction;
    private MessageType type;
    private MessageStatus status;
    private String content;
    private String channelMessageId;
    private String replyToMessageId;
    private String senderName;
    private String senderPhone;
    private String senderEmail;
    private boolean fromBot;
    private SenderType senderType;
    private String botIntent;
    private Float botConfidence;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deliveredAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime readAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime failedAt;
    
    private String failureReason;
    
    private List<MessageAttachmentDto> attachments;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}