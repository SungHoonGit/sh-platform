package com.scraper.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회사 차단 사유 마스터 데이터 — DB 코드화, 프론트 하드코딩 금지.
 * 차단 사유 등록 시 검색/자동완성용으로 조회된다.
 */
@Entity
@Table(name = "block_reasons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlockReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 표시/정렬 순서. 낮을수록 먼저 노출. */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    /** 비활성(0)이면 검색에서 제외. */
    @Column(name = "active", nullable = false)
    private Boolean active;

    public static BlockReason of(String name, Integer sortOrder, Boolean active) {
        var e = new BlockReason();
        e.name = name;
        e.sortOrder = sortOrder;
        e.active = active;
        return e;
    }
}
