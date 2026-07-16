package com.shplatform.auth.infrastructure.tenant;

import com.shplatform.auth.domain.tenant.Tenant;

public class TenantContext {

    private static final ThreadLocal<Tenant> currentTenant = new ThreadLocal<>();

    public static void setCurrent(Tenant tenant) {
        currentTenant.set(tenant);
    }

    public static Tenant getCurrent() {
        return currentTenant.get();
    }

    public static Long getCurrentId() {
        Tenant tenant = currentTenant.get();
        return tenant != null ? tenant.id() : null;
    }

    public static void clear() {
        currentTenant.remove();
    }
}
