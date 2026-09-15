package com.scraper.platform.api.dto;

import com.scraper.platform.model.SiteSearchMapping;
import com.scraper.platform.model.SiteSearchMapping.ValueType;

import java.time.LocalDateTime;

/**
 * 사이트 검색 매핑 조회 응답.
 *
 * @param id            매핑 ID
 * @param siteName      사이트 영문명
 * @param siteDisplayName 사이트 표시명
 * @param standardKey   공통 표준 키
 * @param urlParamName  사이트 URL 파라미터명
 * @param valueType     값 변환 방식
 * @param valueMapping  값 매핑 JSON
 * @param isEnabled     활성 여부
 * @param displayOrder  표시 순서
 * @param updatedAt     수정 시각
 */
public record SearchMappingResponse(
        Long id,
        String siteName,
        String siteDisplayName,
        String standardKey,
        String urlParamName,
        ValueType valueType,
        String valueMapping,
        Boolean isEnabled,
        Integer displayOrder,
        LocalDateTime updatedAt) {

    public static SearchMappingResponse from(SiteSearchMapping m) {
        return new SearchMappingResponse(
                m.getId(),
                m.getSiteDefinition().getSiteName(),
                m.getSiteDefinition().getDisplayName(),
                m.getStandardKey(),
                m.getUrlParamName(),
                m.getValueType(),
                m.getValueMapping(),
                m.getIsEnabled(),
                m.getDisplayOrder(),
                m.getUpdatedAt());
    }
}