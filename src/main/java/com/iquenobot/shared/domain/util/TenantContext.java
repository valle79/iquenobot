package com.iquenobot.shared.domain.util;

public final class TenantContext {

    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(String tenantId) {
        currentTenant.set(tenantId);
    }

    public static String getTenantId() {
        return currentTenant.get();
    }

    public static void setUserId(String userId) {
        currentUser.set(userId);
    }

    public static String getUserId() {
        return currentUser.get();
    }

    public static void clear() {
        currentTenant.remove();
        currentUser.remove();
    }

}