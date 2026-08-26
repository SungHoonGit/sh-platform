package com.shplatform.auth.infrastructure;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_logs", indexes = {
        @Index(name = "idx_admin_audit_actor", columnList = "actor_user_id, created_at"),
        @Index(name = "idx_admin_audit_target", columnList = "target_user_id, created_at"),
        @Index(name = "idx_admin_audit_action", columnList = "action, created_at")
})
public class AdminAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "before_value", length = 500)
    private String beforeValue;

    @Column(name = "after_value", length = 500)
    private String afterValue;

    @Column(length = 45)
    private String ip;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static AdminAuditLogEntity create(Long actorUserId, String action, Long targetUserId,
                                             String beforeValue, String afterValue, String ip) {
        AdminAuditLogEntity entity = new AdminAuditLogEntity();
        entity.actorUserId = actorUserId;
        entity.action = action;
        entity.targetUserId = targetUserId;
        entity.beforeValue = truncate(beforeValue);
        entity.afterValue = truncate(afterValue);
        entity.ip = ip;
        return entity;
    }

    private static String truncate(String value) {
        if (value == null) return null;
        return value.length() > 500 ? value.substring(0, 500) : value;
    }

    public Long getId() { return id; }
    public Long getActorUserId() { return actorUserId; }
    public String getAction() { return action; }
    public Long getTargetUserId() { return targetUserId; }
    public String getBeforeValue() { return beforeValue; }
    public String getAfterValue() { return afterValue; }
    public String getIp() { return ip; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
