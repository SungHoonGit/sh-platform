package com.shplatform.auth.infrastructure.tenant;

import com.shplatform.auth.domain.tenant.TenantMemberRole;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sh_tenant_invitation", indexes = {
        @Index(name = "idx_sh_tenant_invitation_token", columnList = "token"),
        @Index(name = "idx_sh_tenant_invitation_email", columnList = "email")
})
public class TenantInvitationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 200)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantMemberRole role = TenantMemberRole.MEMBER;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime acceptedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static TenantInvitationEntity create(Long tenantId, String email, TenantMemberRole role, String token) {
        var entity = new TenantInvitationEntity();
        entity.tenantId = tenantId;
        entity.email = email;
        entity.role = role;
        entity.token = token;
        entity.expiresAt = LocalDateTime.now().plusDays(7);
        return entity;
    }

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public String getEmail() { return email; }
    public TenantMemberRole getRole() { return role; }
    public void setRole(TenantMemberRole role) { this.role = role; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
