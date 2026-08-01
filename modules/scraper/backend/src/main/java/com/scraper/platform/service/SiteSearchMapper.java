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
     * 값을 매핑 규칙에 따라 변환한다.
     */
    private String convertValue(String value, SiteSearchMapping mapping) {
        return switch (mapping.getValueType()) {
            case direct -> value;
            case mapped, range -> mapValue(value, mapping.getValueMapping());
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
