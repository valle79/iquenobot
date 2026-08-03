package com.iquenobot.admin.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantUniquenessDto {

    private boolean companyNameAvailable;
    private boolean subdomainAvailable;
    private boolean websiteUrlAvailable;
}
