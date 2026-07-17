package com.shplatform.auth.api.admin.dto;

import com.shplatform.auth.domain.tenant.TenantMemberRole;
import com.shplatform.auth.domain.tenant.TenantMemberStatus;
import com.shplatform.auth.domain.tenant.TenantPlanType;
import com.shplatform.auth.domain.tenant.TenantStatus;
import java.time.LocalDateTime;
import java.util.List;

public record TenantDetailResponse(
    Long id,
    String name,
    String slug,
    TenantPlanType planType,
    TenantStatus status,
    int maxUsers,
    List<MemberInfo> members,
    LocalDateTime createdAt
) {
    public record MemberInfo(
        Long userId,
        String name,
        String email,
        TenantMemberRole role,
        TenantMemberStatus status,
        LocalDateTime joinedAt
    ) {}
}
