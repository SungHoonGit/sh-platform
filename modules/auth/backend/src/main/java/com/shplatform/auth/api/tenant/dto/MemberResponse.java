package com.shplatform.auth.api.tenant.dto;

import com.shplatform.auth.domain.tenant.TenantMember;

public record MemberResponse(
        Long id,
        Long tenantId,
        Long userId,
        String role,
        String status,
        String invitedAt,
        String joinedAt,
        String createdAt
) {
    public static MemberResponse from(TenantMember member) {
        return new MemberResponse(
                member.id(),
                member.tenantId(),
                member.userId(),
                member.role().name(),
                member.status().name(),
                member.invitedAt() != null ? member.invitedAt().toString() : null,
                member.joinedAt() != null ? member.joinedAt().toString() : null,
                member.createdAt() != null ? member.createdAt().toString() : null
        );
    }
}
