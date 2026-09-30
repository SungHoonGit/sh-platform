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

    /** 키워드 매칭 방식 (exact=정확일치, contains=부분일치) — 설계 036. 기본 exact = 기존 동작. */
    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 20)
    @Builder.Default
    private MatchType matchType = MatchType.exact;

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

    /**
     * 차단 키워드 매칭 방식 (설계 036).
     * 상수는 소문자 — Jackson 직렬화가 프론트 소문자 유니언(`"exact"|"contains"`)과 일치한다
     * (SiteSearchMapping.ValueType 패턴).
     */
    public enum MatchType {
        /** 정규화명 정확일치 (기존 단일 방식) */
        exact,
        /** 정규화명 부분일치 — 키워드가 포함된 모든 회사 차단 */
        contains
    }
}
