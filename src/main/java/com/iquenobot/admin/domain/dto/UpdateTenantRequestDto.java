package com.iquenobot.admin.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantRequestDto {

    @Size(min = 2, max = 200)
    private String companyName;

    @Size(max = 200)
    private String businessName;

    @Size(max = 20)
    private String ruc;

    @Email
    @Size(max = 255)
    private String contactEmail;

    @Size(max = 20)
    private String contactPhone;

    @Size(max = 500)
    private String websiteUrl;

    @Size(max = 500)
    private String logoUrl;

    @Size(max = 500)
    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String country;

    @Size(max = 50)
    private String timezone;

    @Size(max = 5)
    private String currency;

    @Size(max = 10)
    private String language;

    @Size(max = 10)
    private String locale;

    @Size(min = 4, max = 7)
    private String primaryColor;

    @Size(min = 4, max = 7)
    private String secondaryColor;

    private String subscriptionPlan;
    private Integer maxUsers;
    private Integer maxConversations;
    private UUID planId;
    private LocalDate subscriptionExpiresAt;
}
