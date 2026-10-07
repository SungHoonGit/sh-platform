package com.shplatform.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * (응답형) 앱별 메뉴 조회 결과 — shell AppMenu와 동일 스키마 (DB화 단일 계약).
 *
 * @param app   앱 코드 (scraper / resume / platform / auth)
 * @param items 메뉴 항목 목록 (표시 순서)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MenuResponse(String app, List<Item> items) {

    /**
     * (응답형) 메뉴 항목 1건.
     *
     * @param id       항목 고유 ID (DB PK 대비)
     * @param label    표시 라벨
     * @param href     내부 이동 경로 (external이면 절대 URL)
     * @param icon     lucide 아이콘명 (없으면 null)
     * @param external 외부 링크 여부
     * @param primary  상단 SubNav 노출 여부
     * @param section  드로어 섹션 라벨
     * @param order    표시 순서
     * @param roles    허용 역할 (없으면 전체)
     * @param visible  표시 여부
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Item(String id, String label, String href, String icon, Boolean external,
                       Boolean primary, String section, Integer order, List<String> roles, Boolean visible) {
    }
}
