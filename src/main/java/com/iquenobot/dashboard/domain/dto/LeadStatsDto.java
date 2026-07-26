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
public class LeadStatsDto {

    private long totalLeads;
    private long newLeads;
    private long contactedLeads;
    private long qualifiedLeads;
    private long convertedLeads;
    private long lostLeads;
    private BigDecimal conversionRate;
    private BigDecimal averageLeadScore;
    private long leadsToday;
    private long leadsThisWeek;
    private long leadsThisMonth;
    private long unassignedLeads;
    private long highScoreLeads;
}
