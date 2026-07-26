package com.iquenobot.admin.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatsDto {
    private long totalTenants;
    private long activeTenants;
    private long suspendedTenants;
    private long trialTenants;
    private long expiredTenants;
    private long totalUsers;
    private long totalConversations;
    private long totalMessages;
    private long totalContacts;
    private long totalLeads;
    private long totalProducts;
    private double storageUsedMb;
    private long aiTotalRequests;
    private long aiTotalTokens;
    private ServerStatusDto server;
    private DatabaseStatusDto database;
    private IntegrationStatusDto evolutionApi;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerStatusDto {
        private String status;
        private String version;
        private String uptime;
        private double cpuUsage;
        private double memoryUsage;
        private double memoryMax;
        private int activeThreads;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseStatusDto {
        private String status;
        private int activeConnections;
        private int maxConnections;
        private long diskUsageMb;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntegrationStatusDto {
        private String status;
        private String lastCheck;
        private String errorMessage;
    }
}
