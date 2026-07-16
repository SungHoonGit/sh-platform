package com.shplatform.auth.domain.tenant;

import java.time.LocalDateTime;
import java.util.Map;

public record Tenant(
        Long id,
        String name,
        String slug,
        String domain,
        String logoUrl,
        TenantStatus status,
        TenantPlanType planType,
        int maxUsers,
        Map<String, Object> settings,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static Tenant create(String name, String slug) {
        return new Tenant(
                null, name, slug, null, null,
                TenantStatus.ACTIVE, TenantPlanType.FREE, 5,
                null, null, null
        );
    }

    public Tenant suspend() {
        return new Tenant(
                id, name, slug, domain, logoUrl,
                TenantStatus.SUSPENDED, planType, maxUsers,
                settings, createdAt, LocalDateTime.now()
        );
    }

    public Tenant activate() {
        return new Tenant(
                id, name, slug, domain, logoUrl,
                TenantStatus.ACTIVE, planType, maxUsers,
                settings, createdAt, LocalDateTime.now()
        );
    }
}
