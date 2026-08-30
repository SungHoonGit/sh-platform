package com.scraper.platform.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    /** 자유 텍스트 메모(선택). 카테고리는 {@link #blockReasons} 다대다로 저장된다. */
    @Column(length = 200)
    private String reason;

    /** 선택한 차단 카테고리(회사유형 + 사유) — 다대다 연결 테이블 blacklist_block_reason */
    @ManyToMany
    @JoinTable(name = "blacklist_block_reason",
            joinColumns = @JoinColumn(name = "blacklist_id"),
            inverseJoinColumns = @JoinColumn(name = "block_reason_id"))
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<BlockReason> blockReasons = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
