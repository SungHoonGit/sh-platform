package com.scraper.platform.controller;

import com.scraper.platform.model.CrawlSiteConfig;
import com.scraper.platform.service.CrawlSiteConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/crawl-config/{configId}/site-configs")
@RequiredArgsConstructor
@Tag(name = "CrawlSiteConfig", description = "사이트별 크롤링 설정 API")
public class CrawlSiteConfigController {

    private final CrawlSiteConfigService crawlSiteConfigService;

    @GetMapping
    @Operation(summary = "사이트 설정 목록 조회", description = "특정 설정의 사이트별 설정 목록을 조회합니다")
    public ResponseEntity<List<CrawlSiteConfig>> getSiteConfigs(@PathVariable Long configId) {
        return ResponseEntity.ok(crawlSiteConfigService.getConfigSiteConfigs(configId));
    }

    @GetMapping("/enabled")
    @Operation(summary = "활성 사이트 설정 조회", description = "활성화된 사이트 설정만 조회합니다")
    public ResponseEntity<List<CrawlSiteConfig>> getEnabledSiteConfigs(@PathVariable Long configId) {
        return ResponseEntity.ok(crawlSiteConfigService.getEnabledSiteConfigs(configId));
    }

    @GetMapping("/{siteDefinitionId}")
    @Operation(summary = "사이트 설정 상세 조회", description = "특정 사이트의 설정을 조회합니다")
    public ResponseEntity<CrawlSiteConfig> getSiteConfig(
            @PathVariable Long configId,
            @PathVariable Long siteDefinitionId) {
        return ResponseEntity.ok(crawlSiteConfigService.getSiteConfig(configId, siteDefinitionId));
    }

    @PostMapping("/{siteDefinitionId}")
    @Operation(summary = "사이트 설정 생성/수정", description = "사이트별 크롤링 설정을 생성하거나 수정합니다")
    public ResponseEntity<CrawlSiteConfig> createOrUpdateSiteConfig(
            @PathVariable Long configId,
            @PathVariable Long siteDefinitionId,
            @RequestBody CrawlSiteConfig siteConfig) {
        return ResponseEntity.ok(crawlSiteConfigService.createOrUpdateSiteConfig(configId, siteDefinitionId, siteConfig));
    }

    @DeleteMapping("/{siteDefinitionId}")
    @Operation(summary = "사이트 설정 삭제", description = "사이트별 크롤링 설정을 삭제합니다")
    public ResponseEntity<Void> deleteSiteConfig(
            @PathVariable Long configId,
            @PathVariable Long siteDefinitionId) {
        crawlSiteConfigService.deleteSiteConfig(configId, siteDefinitionId);
        return ResponseEntity.ok().build();
    }
}
