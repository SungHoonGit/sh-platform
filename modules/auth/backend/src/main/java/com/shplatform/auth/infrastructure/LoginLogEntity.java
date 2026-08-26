package com.shplatform.auth.infrastructure;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_logs", indexes = {
        @Index(name = "idx_login_logs_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_login_logs_email_created", columnList = "email, created_at")
})
public class LoginLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false)
    private String email;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(nullable = false)
    private boolean success;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static LoginLogEntity create(Long userId, String email, String ip,
                                        String userAgent, boolean success) {
        LoginLogEntity entity = new LoginLogEntity();
        entity.userId = userId;
        entity.email = email != null ? email : "unknown";
        entity.ip = ip;
        entity.userAgent = userAgent != null && userAgent.length() > 512
                ? userAgent.substring(0, 512) : userAgent;
        entity.success = success;
        return entity;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getIp() { return ip; }
    public String getUserAgent() { return userAgent; }
    public boolean isSuccess() { return success; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
