package com.shplatform.auth.api.tenant.dto;

import com.shplatform.auth.domain.tenant.Tenant;

public record TenantResponse(
        Long id,
        String name,
        String slug,
        String domain,
        String logoUrl,
        String status,
        String planType,
        int maxUsers,
        String createdAt,
        String updatedAt
) {
    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.id(),
                tenant.name(),
                tenant.slug(),
                tenant.domain(),
                tenant.logoUrl(),
                tenant.status().name(),
                tenant.planType().name(),
                tenant.maxUsers(),
                tenant.createdAt() != null ? tenant.createdAt().toString() : null,
                tenant.updatedAt() != null ? tenant.updatedAt().toString() : null
        );
    }
}
