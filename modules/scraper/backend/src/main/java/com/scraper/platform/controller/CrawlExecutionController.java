package com.scraper.platform.controller;

import com.scraper.platform.model.CrawlConfig;
import com.scraper.platform.service.CrawlConfigService;
import com.scraper.platform.service.CrawlExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/crawl-config")
@RequiredArgsConstructor
@Tag(name = "CrawlExecution", description = "크롤링 실행 API")
public class CrawlExecutionController {

    private final CrawlExecutionService crawlExecutionService;
    private final CrawlConfigService crawlConfigService;

    @PostMapping("/{id}/execute")
    @Operation(summary = "크롤링 수동 실행", description = "지정된 설정으로 크롤링을 수동 실행합니다")
    public ResponseEntity<Map<String, String>> executeCrawl(@PathVariable Long id) {
        CrawlConfig config = crawlConfigService.getConfigById(id);

        CompletableFuture.runAsync(() -> {
            try {
                crawlExecutionService.executeCrawl(config);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(CrawlExecutionController.class)
                    .error("Async crawl failed for config: {}", config.getName(), e);
            }
        });

        return ResponseEntity.ok(Map.of(
            "status", "started",
            "message", "Crawl execution started for config: " + config.getName()
        ));
    }
}
