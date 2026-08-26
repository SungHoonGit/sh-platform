package com.scraper.platform.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "company_blacklist", uniqueConstraints = {
        @UniqueConstraint(name = "uk_blacklist_account_company", columnNames = {"account_id", "company_name_normalized"})
}, indexes = {
        @Index(name = "idx_blacklist_account", columnList = "account_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "company_name_normalized", nullable = false, length = 200)
    private String companyNameNormalized;

    @Column(length = 200)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
