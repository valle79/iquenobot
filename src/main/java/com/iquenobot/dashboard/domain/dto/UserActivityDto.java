package com.iquenobot.dashboard.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityDto {

    private UUID userId;
    private String userName;
    private String userEmail;
    private long assignedConversations;
    private long closedConversations;
    private long assignedLeads;
    private long convertedLeads;
    private long sentMessages;
}
