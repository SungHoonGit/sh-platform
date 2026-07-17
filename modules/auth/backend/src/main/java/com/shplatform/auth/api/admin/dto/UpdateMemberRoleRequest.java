package com.shplatform.auth.api.admin.dto;

import com.shplatform.auth.domain.tenant.TenantMemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
    @NotNull TenantMemberRole role
) {}
