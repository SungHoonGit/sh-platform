package com.shplatform.auth.infrastructure.tenant;

import com.shplatform.auth.domain.tenant.TenantPlanType;
import com.shplatform.auth.domain.tenant.TenantStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sh_tenant", indexes = {
        @Index(name = "idx_sh_tenant_slug", columnList = "slug"),
        @Index(name = "idx_sh_tenant_status", columnList = "status")
})
public class TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    @Column(length = 100)
    private String domain;

    @Column(length = 500)
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantPlanType planType = TenantPlanType.FREE;

    @Column(nullable = false)
    private int maxUsers = 5;

    @Column(columnDefinition = "JSON")
    private String settings;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static TenantEntity create(String name, String slug) {
        var entity = new TenantEntity();
        entity.name = name;
        entity.slug = slug;
        return entity;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }
    public TenantPlanType getPlanType() { return planType; }
    public void setPlanType(TenantPlanType planType) { this.planType = planType; }
    public int getMaxUsers() { return maxUsers; }
    public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }
    public String getSettings() { return settings; }
    public void setSettings(String settings) { this.settings = settings; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
