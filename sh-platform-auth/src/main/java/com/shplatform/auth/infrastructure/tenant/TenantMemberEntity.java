package com.shplatform.auth.infrastructure.tenant;

import com.shplatform.auth.domain.tenant.TenantMemberRole;
import com.shplatform.auth.domain.tenant.TenantMemberStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sh_tenant_member", indexes = {
        @Index(name = "idx_sh_tenant_member_user", columnList = "userId"),
        @Index(name = "idx_sh_tenant_member_tenant", columnList = "tenantId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_user", columnNames = {"tenantId", "userId"})
})
public class TenantMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantMemberRole role = TenantMemberRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantMemberStatus status = TenantMemberStatus.INVITED;

    private LocalDateTime invitedAt;

    private LocalDateTime joinedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenantId", insertable = false, updatable = false)
    private TenantEntity tenant;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static TenantMemberEntity create(Long tenantId, Long userId, TenantMemberRole role) {
        var entity = new TenantMemberEntity();
        entity.tenantId = tenantId;
        entity.userId = userId;
        entity.role = role;
        entity.invitedAt = LocalDateTime.now();
        return entity;
    }

    public void accept() {
        this.status = TenantMemberStatus.ACTIVE;
        this.joinedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public Long getUserId() { return userId; }
    public TenantMemberRole getRole() { return role; }
    public void setRole(TenantMemberRole role) { this.role = role; }
    public TenantMemberStatus getStatus() { return status; }
    public void setStatus(TenantMemberStatus status) { this.status = status; }
    public LocalDateTime getInvitedAt() { return invitedAt; }
    public void setInvitedAt(LocalDateTime invitedAt) { this.invitedAt = invitedAt; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public TenantEntity getTenant() { return tenant; }
}
