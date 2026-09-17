package com.scraper.platform.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 계정별 회사 메모. 내 별점(1~5), 북마크, 마크다운 분석 메모를 회사당 1행으로 저장한다.
 * 크롤링 평점은 {@code company_ratings}(전역), 차단은 {@code company_blacklist}(계정별)를 조인 조회한다.
 */
@Entity
@Table(name = "company_notes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_company_notes_account_company",
                columnNames = {"account_id", "company_name_normalized"})
}, indexes = {
        @Index(name = "idx_company_notes_account", columnList = "account_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "company_name_normalized", nullable = false, length = 200)
    private String companyNameNormalized;

    /** 화면 표시용 원문 회사명 (최초 입력값, trim). */
    @Column(name = "company_name_display", nullable = false, length = 200)
    private String companyNameDisplay;

    /** 내 별점 1~5, null이면 미지정. */
    @Column(name = "my_stars")
    private Integer myStars;

    @Column(name = "is_bookmarked", nullable = false)
    @Builder.Default
    private Boolean isBookmarked = false;

    /** 분석 마크다운 원문. */
    @Column(name = "note_md", columnDefinition = "MEDIUMTEXT")
    private String noteMd;

    /** 분석 메모용 카테고리/태그 — block_reasons 마스터 공유 (북마크 회사에도 부여 가능). */
    @ManyToMany
    @JoinTable(name = "company_note_reason",
            joinColumns = @JoinColumn(name = "note_id"),
            inverseJoinColumns = @JoinColumn(name = "block_reason_id"))
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<BlockReason> noteReasons = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
