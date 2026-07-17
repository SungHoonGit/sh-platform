package com.shplatform.auth.api.admin.dto;

import com.shplatform.auth.domain.tenant.TenantPlanType;
import com.shplatform.auth.domain.tenant.TenantStatus;
import java.time.LocalDateTime;

public record TenantListResponse(
    Long id,
    String name,
    String slug,
    TenantPlanType planType,
    TenantStatus status,
    int maxUsers,
    long memberCount,
    LocalDateTime createdAt
) {}
