package com.shplatform.auth.api.admin.dto;

import com.shplatform.auth.domain.tenant.TenantPlanType;
import com.shplatform.auth.domain.tenant.TenantStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTenantRequest(
    @NotNull TenantPlanType planType,
    @NotNull TenantStatus status
) {}
