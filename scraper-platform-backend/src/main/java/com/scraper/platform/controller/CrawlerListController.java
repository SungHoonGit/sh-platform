package com.scraper.platform.controller;

import com.scraper.platform.model.CrawlConfig;
import com.scraper.platform.repository.CrawlConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @Operation(summary = "활성 크롤러 목록 조회", description = "활성화된 크롤러 설정 목록을 조회합니다. 문서 뷰어에서 크롤러 선택에 사용됩니다.")
    public ResponseEntity<List<Map<String, Object>>> getCrawlers() {
        List<CrawlConfig> configs = crawlConfigRepository.findByIsActiveTrue();
        
        List<Map<String, Object>> result = configs.stream()
            .map(config -> Map.<String, Object>of(
                "id", config.getId(),
                "name", config.getName(),
                "localPath", config.getLocalPath() != null ? config.getLocalPath() : "",
                "schedule", config.getSchedule() != null ? config.getSchedule() : ""
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
}
