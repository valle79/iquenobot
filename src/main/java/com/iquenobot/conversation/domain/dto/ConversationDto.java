package com.iquenobot.conversation.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.iquenobot.auth.domain.dto.UserDto;
import com.iquenobot.contact.domain.dto.ContactDto;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.ConversationStatus;
import com.iquenobot.shared.enums.ConversationPriority;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationDto {

    private UUID id;
    private ContactDto contact;
    private UserDto assignedUser;
    private ChannelType channel;
    private ConversationStatus status;
    private ConversationPriority priority;
    private String subject;
    private String channelConversationId;
    private boolean isGroup;
    private String instanceName;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastMessageAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime firstResponseAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime resolvedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime closedAt;
    
    private Long responseTimeSeconds;
    private Long resolutionTimeSeconds;
    private Integer messageCount;
    private Integer unreadCount;
    private Integer satisfactionRating;
    private String satisfactionFeedback;
    private String tags;
    private String metadata;
    private boolean botConversation;
    private boolean humanHandoff;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime humanTakenOverAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastAgentReplyAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime botResumeAfter;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime botHandoffAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private ConversationMessageDto lastMessage;
}