package com.iquenobot.auth.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.iquenobot.plan.domain.dto.PlanDto;
import com.iquenobot.shared.enums.TenantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantDto {

    private UUID id;
    private String companyName;
    private String businessName;
    private String ruc;
    private String subdomain;
    private String contactEmail;
    private String contactPhone;
    private String websiteUrl;
    private String logoUrl;
    private String address;
    private String city;
    private String country;
    private String timezone;
    private String currency;
    private String language;
    private String locale;
    private String primaryColor;
    private String secondaryColor;
    private TenantStatus status;
    private String subscriptionPlan;
    private PlanDto plan;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate subscriptionExpiresAt;

    private Integer maxUsers;
    private Integer maxConversations;
    private Integer maxAgents;
    private Integer maxSupervisors;
    private String features;
    private int userCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
