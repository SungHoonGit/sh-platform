package com.scraper.platform.controller;

import com.scraper.platform.model.CrawlConfig;
import com.scraper.platform.service.CrawlConfigService;
import com.scraper.platform.service.CrawlExecutionService;
import com.scraper.platform.service.CrawlProgressBroadcaster;
import com.shplatform.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/crawl-config")
@RequiredArgsConstructor
@Tag(name = "CrawlExecution", description = "크롤링 실행 API")
public class CrawlExecutionController {

    private final CrawlExecutionService crawlExecutionService;
    private final CrawlConfigService crawlConfigService;
    private final CrawlProgressBroadcaster progressBroadcaster;

    @PostMapping("/{id}/execute")
    @Operation(summary = "크롤링 수동 실행", description = "지정된 설정으로 크롤링을 수동 실행합니다")
    public ResponseEntity<Map<String, String>> executeCrawl(@PathVariable Long id) {
        CrawlConfig config = crawlConfigService.getConfigById(id, SecurityUtils.currentAccountId());

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

    @GetMapping(value = "/{id}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "크롤링 진행 상태 SSE", description = "크롤링 진행 상황을 실시간으로 수신합니다")
    public SseEmitter getCrawlProgress(@PathVariable Long id) {
        return progressBroadcaster.register(id);
    }
}
