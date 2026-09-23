package com.scraper.platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scraper.platform.model.SiteSearchMapping;
import com.scraper.platform.repository.SiteSearchMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 표준 검색 파라미터를 사이트별 URL 파라미터로 변환하는 서비스.
 * <p>
 * 예시: {"career":"3~5년","location":"서울"} → {"career_level":"5","loc_cd":"101000"}
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteSearchMapper {

    private final SiteSearchMappingRepository mappingRepository;
    private final ObjectMapper objectMapper;

    /**
     * 표준 paramValues를 사이트별 URL 파라미터 Map으로 변환한다.
     *
     * @param siteName 사이트 영문명 (saramin, jobkorea, wanted, remember)
     * @param paramValues JSON 문자열 (예: {"keyword":"React","career":"3~5년","location":"서울"})
     * @return 사이트별 URL 파라미터 (예: {"stext":"React","career_level":"5","loc_cd":"101000"})
     */
    public Map<String, String> toSiteParams(String siteName, String paramValues) {
        Map<String, String> standardParams = parseParamValues(paramValues);
        if (standardParams.isEmpty()) {
            return Map.of();
        }

        List<SiteSearchMapping> mappings = mappingRepository
                .findBySiteDefinition_SiteNameAndIsEnabledTrueOrderByDisplayOrder(siteName);

        Map<String, String> siteParams = new LinkedHashMap<>();
        for (SiteSearchMapping mapping : mappings) {
            String value = standardParams.get(mapping.getStandardKey());
            if (value == null || value.isEmpty()) continue;

            String converted = convertValue(value, mapping);
            if (converted != null && !converted.isEmpty()) {
                siteParams.put(mapping.getUrlParamName(), converted);
            }
        }

        log.debug("SiteSearchMapper: {} params converted: {} -> {}", siteName, standardParams, siteParams);
        return siteParams;
    }

    /**
     * 사이트명과 표준 파라미터 Map을 받아 사이트별 URL 파라미터 Map을 반환한다.
     * 크롤러에서 직접 사용할 수 있는 편의 메서드.
     *
     * @param siteName 사이트 영문명
     * @param standardParams 표준 파라미터 맵 (keyword, career, location 등)
     * @return 사이트별 URL 파라미터 맵
     */
    public Map<String, String> toSiteParams(String siteName, Map<String, String> standardParams) {
        if (standardParams == null || standardParams.isEmpty()) {
            return Map.of();
        }

        List<SiteSearchMapping> mappings = mappingRepository
                .findBySiteDefinition_SiteNameAndIsEnabledTrueOrderByDisplayOrder(siteName);

        Map<String, String> siteParams = new LinkedHashMap<>();
        for (SiteSearchMapping mapping : mappings) {
            String value = standardParams.get(mapping.getStandardKey());
            if (value == null || value.isEmpty()) continue;

            String converted = convertValue(value, mapping);
            if (converted != null && !converted.isEmpty()) {
                siteParams.put(mapping.getUrlParamName(), converted);
            }
        }

        log.debug("SiteSearchMapper: {} params converted: {} -> {}", siteName, standardParams, siteParams);
        return siteParams;
    }

    /**
     * 사이트별 지역코드를 value_mapping DB 기준으로 변환한다.
     * <p>
     * 사이트의 {@code standard_key=location} 매핑에 값이 있으면 그 코드를 반환하고,
     * 매핑 행이나 지역 값이 없으면 빈 문자열을 반환한다. 크롤러 호출부에서
     * 반환값이 빈 문자열일 경우 기존 fallback(하드코딩 switch)을 사용한다.
     *
     * @param siteName 사이트 영문명 (saramin, jobkorea, wanted, remember)
     * @param location 표준 지역명 (예: "서울")
     * @return 사이트별 지역코드 (예: saramin "101000"), 없으면 빈 문자열
     */
    public String mapLocationCode(String siteName, String location) {
        if (location == null || location.isEmpty()) {
            return "";
        }
        Optional<SiteSearchMapping> mapping = mappingRepository
                .findBySiteDefinition_SiteNameAndStandardKeyAndIsEnabledTrue(siteName, "location");
        if (mapping.isEmpty()) {
            return "";
        }
        String converted = mapValue(location, mapping.get().getValueMapping());
        return converted == null ? "" : converted;
    }

    /**
     * 표준 키의 compound 매핑에서 값을 사이트별 파라미터 Map으로 확장한다.
     * <p>
     * 예: saramin/career/"1~3년" → {@code {exp_cd=2, exp_min=1, exp_max=3}}
     * <p>
     * 행 부재·비활성·value_type이 compound가 아님·JSON 파싱 실패·표준값 미포함은
     * 모두 빈 Map을 반환한다 — 호출부(크롤러)에서 하드코딩 fallback을 쓰도록 (설계 032).
     *
     * @param siteName    사이트 영문명 (saramin, jobkorea)
     * @param standardKey 표준 키 (career 등)
     * @param value       표준값 (예: "1~3년")
     * @return 사이트 URL 파라미터 Map, 사용 불가 시 빈 Map
     */
    public Map<String, String> mapCompoundParams(String siteName, String standardKey, String value) {
        if (siteName == null || standardKey == null || value == null || value.isEmpty()) {
            return Map.of();
        }
        Optional<SiteSearchMapping> mapping = mappingRepository
                .findBySiteDefinition_SiteNameAndStandardKeyAndIsEnabledTrue(siteName, standardKey);
        if (mapping.isEmpty() || mapping.get().getValueType() != SiteSearchMapping.ValueType.compound) {
            return Map.of();
        }
        String valueMappingJson = mapping.get().getValueMapping();
        if (valueMappingJson == null || valueMappingJson.isBlank()) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(valueMappingJson);
            JsonNode entry = root.get(value);
            if (entry == null || !entry.isObject()) {
                return Map.of();
            }
            Map<String, String> params = new LinkedHashMap<>();
            entry.fields().forEachRemaining(e -> params.put(e.getKey(), e.getValue().asText()));
            return params;
        } catch (Exception e) {
            log.warn("Failed to parse compound value_mapping for {}/{}: {}", siteName, standardKey, valueMappingJson, e);
            return Map.of();
        }
    }

    /**
     * 값을 매핑 규칙에 따라 변환한다.
     * compound는 복수 파라미터이므로 단일 문자열 변환 경로에서는 스킵한다
     * (크롤러는 {@link #mapCompoundParams}로 직접 조회).
     */
    private String convertValue(String value, SiteSearchMapping mapping) {
        return switch (mapping.getValueType()) {
            case direct -> value;
            case mapped, range -> mapValue(value, mapping.getValueMapping());
            case compound -> null;
        };
    }

    /**
     * value_mapping JSON에서 값을 찾아 코드로 변환한다.
     * 콤마로 구분된 다중 값("서울,경기")은 각각 변환 후 콤마로 다시 연결한다.
     * 매핑에 없는 값이 하나라도 있으면 null을 반환한다.
     */
    private String mapValue(String value, String valueMappingJson) {
        if (valueMappingJson == null) return value;
        try {
            JsonNode node = objectMapper.readTree(valueMappingJson);
            String[] parts = value.split(",");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                String trimmed = part.trim();
                JsonNode mapped = node.get(trimmed);
                if (mapped == null) return null;
                if (sb.length() > 0) sb.append(",");
                sb.append(mapped.asText());
            }
            return sb.length() == 0 ? null : sb.toString();
        } catch (Exception e) {
            log.warn("Failed to parse value_mapping: {}", valueMappingJson, e);
            return null;
        }
    }

    /**
     * JSON 문자열을 표준 파라미터 맵으로 파싱한다.
     */
    private Map<String, String> parseParamValues(String paramValues) {
        if (paramValues == null || paramValues.isEmpty()) return Map.of();
        try {
            JsonNode node = objectMapper.readTree(paramValues);
            Map<String, String> params = new HashMap<>();
            node.fields().forEachRemaining(e -> params.put(e.getKey(), e.getValue().asText()));
            return params;
        } catch (Exception e) {
            log.warn("Failed to parse paramValues: {}", paramValues, e);
            return Map.of();
        }
    }
}
