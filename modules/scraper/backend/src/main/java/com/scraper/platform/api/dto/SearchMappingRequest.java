package com.scraper.platform.api.dto;

import com.scraper.platform.model.SiteSearchMapping.ValueType;

/**
 * 사이트 검색 매핑 생성/수정 요청.
 *
 * @param siteDefinitionId 대상 사이트 ID
 * @param standardKey        공통 표준 키 (keyword, career, location, job_type)
 * @param urlParamName       사이트 URL 파라미터명 (stext, loc_cd, career_level 등)
 * @param valueType          값 변환 방식 (direct, mapped, range)
 * @param valueMapping       값 매핑 JSON ({@code mapped}/{@code range} 필수)
 * @param isEnabled          활성 여부
 * @param displayOrder       표시 순서
 */
public record SearchMappingRequest(
        Long siteDefinitionId,
        String standardKey,
        String urlParamName,
        ValueType valueType,
        String valueMapping,
        Boolean isEnabled,
        Integer displayOrder) {
}