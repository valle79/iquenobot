package com.iquenobot.dashboard.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationStatsDto {

    private long totalConversations;
    private long activeConversations;
    private long openConversations;
    private long closedConversations;
    private long pendingConversations;
    private BigDecimal averageResponseTimeMinutes;
    private BigDecimal averageResolutionTimeHours;
    private long conversationsToday;
    private long conversationsThisWeek;
    private long conversationsThisMonth;
}
