package com.scraper.platform.controller;

import com.scraper.platform.model.CrawlConfig;
import com.scraper.platform.service.CrawlConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/crawl-config")
@RequiredArgsConstructor
@Tag(name = "CrawlConfig", description = "크롤링 설정 관리 API")
public class CrawlConfigController {

    private final CrawlConfigService crawlConfigService;

    @GetMapping
    @Operation(summary = "전체 설정 조회", description = "크롤링 설정 목록을 조회합니다")
    public ResponseEntity<List<CrawlConfig>> getAllConfigs() {
        return ResponseEntity.ok(crawlConfigService.getAllConfigs());
    }

    @GetMapping("/{id}")
    @Operation(summary = "설정 상세 조회", description = "ID로 크롤링 설정을 조회합니다")
    public ResponseEntity<CrawlConfig> getConfigById(@PathVariable Long id) {
        return ResponseEntity.ok(crawlConfigService.getConfigById(id));
    }

    @GetMapping("/active")
    @Operation(summary = "활성 설정 조회", description = "활성화된 크롤링 설정만 조회합니다")
    public ResponseEntity<List<CrawlConfig>> getActiveConfigs() {
        return ResponseEntity.ok(crawlConfigService.getActiveConfigs());
    }

    @PostMapping
    @Operation(summary = "설정 생성", description = "새로운 크롤링 설정을 생성합니다")
    public ResponseEntity<CrawlConfig> createConfig(@RequestBody CrawlConfig config) {
        return ResponseEntity.ok(crawlConfigService.createConfig(config));
    }

    @PutMapping("/{id}")
    @Operation(summary = "설정 수정", description = "크롤링 설정을 수정합니다")
    public ResponseEntity<CrawlConfig> updateConfig(
            @PathVariable Long id,
            @RequestBody CrawlConfig config) {
        return ResponseEntity.ok(crawlConfigService.updateConfig(id, config));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "설정 삭제", description = "크롤링 설정을 삭제합니다")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        crawlConfigService.deleteConfig(id);
        return ResponseEntity.ok().build();
    }
}
