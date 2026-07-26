package com.iquenobot.dashboard.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDto {

    private ConversationStatsDto conversationStats;
    private LeadStatsDto leadStats;
    private ContactStatsDto contactStats;
    private ProductStatsDto productStats;
}
