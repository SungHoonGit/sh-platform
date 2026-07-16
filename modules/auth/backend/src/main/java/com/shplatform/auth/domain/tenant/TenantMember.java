package com.shplatform.auth.domain.tenant;

import java.time.LocalDateTime;

public record TenantMember(
        Long id,
        Long tenantId,
        Long userId,
        TenantMemberRole role,
        TenantMemberStatus status,
        LocalDateTime invitedAt,
        LocalDateTime joinedAt,
        LocalDateTime createdAt
) {
    public static TenantMember create(Long tenantId, Long userId, TenantMemberRole role) {
        return new TenantMember(
                null, tenantId, userId, role,
                TenantMemberStatus.INVITED,
                LocalDateTime.now(), null, null
        );
    }

    public TenantMember accept() {
        return new TenantMember(
                id, tenantId, userId, role,
                TenantMemberStatus.ACTIVE,
                invitedAt, LocalDateTime.now(), createdAt
        );
    }

    public TenantMember changeRole(TenantMemberRole newRole) {
        return new TenantMember(
                id, tenantId, userId, newRole,
                status, invitedAt, joinedAt, createdAt
        );
    }
}
