package com.scraper.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 사이트별 검색 파라미터 매핑 엔티티.
 * 표준 키(keyword, career, location 등)를 사이트별 URL 파라미터로 변환하는 규칙을 정의한다.
 */
@Entity
@Table(name = "site_search_mapping",
       uniqueConstraints = @UniqueConstraint(columnNames = {"site_definition_id", "standard_key"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SiteSearchMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_definition_id", nullable = false)
    private SiteDefinition siteDefinition;

    /** 공통 표준 키 (keyword, career, location, job_type) */
    @Column(name = "standard_key", nullable = false, length = 50)
    private String standardKey;

    /** 사이트 URL 파라미터명 (stext, loc_cd, career_level 등) */
    @Column(name = "url_param_name", nullable = false, length = 100)
    private String urlParamName;

    /** 값 변환 방식 (direct, mapped, range) */
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private ValueType valueType;

    /** 값 매핑 JSON (예: {"3~5년":"5","5~10년":"8"}) */
    @Column(name = "value_mapping", columnDefinition = "JSON")
    private String valueMapping;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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

    public enum ValueType {
        /** 값을 그대로 URL에 전달 (예: 키워드 "React" → stext=React) */
        direct,
        /** value_mapping JSON으로 코드 변환 (예: "3~5년" → career_level=5) */
        mapped,
        /** 범위 파라미터로 변환 (예: "3~5년" → years=3) */
        range
    }
}
