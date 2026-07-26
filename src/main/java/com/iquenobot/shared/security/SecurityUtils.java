package com.iquenobot.shared.security;

import com.iquenobot.shared.domain.util.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }

    public static UUID getCurrentUserId() {
        String userId = TenantContext.getUserId();
        return userId != null ? UUID.fromString(userId) : null;
    }

    public static UUID getCurrentTenantId() {
        String tenantId = TenantContext.getTenantId();
        return tenantId != null ? UUID.fromString(tenantId) : null;
    }

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && 
               !"anonymousUser".equals(authentication.getPrincipal());
    }
}