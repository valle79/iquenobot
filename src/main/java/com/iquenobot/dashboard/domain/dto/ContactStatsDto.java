package com.iquenobot.dashboard.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactStatsDto {

    private long totalContacts;
    private long activeContacts;
    private long blockedContacts;
    private long subscribedContacts;
    private long contactsToday;
    private long contactsThisWeek;
    private long contactsThisMonth;
    private long contactsWithConversations;
}
