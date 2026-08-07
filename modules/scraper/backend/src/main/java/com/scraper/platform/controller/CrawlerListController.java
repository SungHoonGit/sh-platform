package com.scraper.platform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scraper.platform.model.CrawlConfig;
import com.scraper.platform.repository.CrawlConfigRepository;
import com.shplatform.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/docs/crawlers")
@Tag(name = "CrawlerList", description = "크롤러 목록 조회 API")
public class CrawlerListController {

    private final CrawlConfigRepository crawlConfigRepository;

    public CrawlerListController(CrawlConfigRepository crawlConfigRepository) {
        this.crawlConfigRepository = crawlConfigRepository;
    }

    @GetMapping
    @Operation(summary = "활성 크롤러 목록 조회", description = "현재 사용자의 활성화된 크롤러 설정 목록을 조회합니다. 문서 뷰어에서 크롤러 선택에 사용됩니다.")
    public ResponseEntity<List<Map<String, Object>>> getCrawlers() {
        List<CrawlConfig> configs = crawlConfigRepository
                .findByAccountIdAndIsActiveTrue(SecurityUtils.currentAccountId());
        
        List<Map<String, Object>> result = configs.stream()
            .map(config -> {
                List<Map<String, Object>> siteConfigs = config.getSiteConfigs().stream()
                    .map(sc -> Map.<String, Object>of(
                        "siteName", sc.getSiteDefinition().getSiteName(),
                        "displayName", sc.getSiteDefinition().getDisplayName(),
                        "baseUrl", sc.getSiteDefinition().getBaseUrl() != null ? sc.getSiteDefinition().getBaseUrl() : "",
                        "isEnabled", sc.getIsEnabled(),
                        "paramValues", sc.getParamValues() != null ? sc.getParamValues() : "{}"
                    ))
                    .collect(Collectors.toList());
                
                Map<String, Object> searchCriteria = extractSearchCriteria(config.getSiteConfigs());
                
                return Map.<String, Object>of(
                    "id", config.getId(),
                    "name", config.getName(),
                    "localPath", config.getLocalPath() != null ? config.getLocalPath() : "",
                    "schedule", config.getSchedule() != null ? config.getSchedule() : "",
                    "scheduleIcon", config.getScheduleIcon() != null ? config.getScheduleIcon() : "🤖",
                    "siteConfigs", siteConfigs,
                    "searchCriteria", searchCriteria
                );
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> extractSearchCriteria(List<com.scraper.platform.model.CrawlSiteConfig> siteConfigs) {
        Map<String, Object> criteria = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        
        for (com.scraper.platform.model.CrawlSiteConfig siteConfig : siteConfigs) {
            if (siteConfig.getParamValues() == null || siteConfig.getParamValues().isEmpty()) continue;
            
            try {
                JsonNode node = mapper.readTree(siteConfig.getParamValues());
                if (node.has("keyword") && !node.get("keyword").asText().isEmpty()) {
                    criteria.putIfAbsent("keyword", node.get("keyword").asText());
                }
                if (node.has("career") && !node.get("career").asText().isEmpty()) {
                    criteria.putIfAbsent("career", node.get("career").asText());
                }
                if (node.has("location") && !node.get("location").asText().isEmpty()) {
                    criteria.putIfAbsent("location", node.get("location").asText());
                }
            } catch (Exception e) {
                // JSON 파싱 실패 시 무시
            }
        }
        
        return criteria;
    }
}
