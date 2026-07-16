package com.scraper.platform.controller;

import com.scraper.platform.model.CrawlLog;
import com.scraper.platform.service.CrawlLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/crawl-logs")
@RequiredArgsConstructor
@Tag(name = "CrawlLog", description = "크롤링 로그 API")
public class CrawlLogController {

    private final CrawlLogService crawlLogService;

    @GetMapping("/config/{configId}")
    @Operation(summary = "설정별 로그 조회", description = "특정 설정의 크롤링 로그를 페이징하여 조회합니다")
    public ResponseEntity<Page<CrawlLog>> getLogsByConfigId(
            @PathVariable Long configId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("startedAt").descending());
        return ResponseEntity.ok(crawlLogService.getLogsByConfigId(configId, pageRequest));
    }

    @GetMapping("/config/{configId}/recent")
    @Operation(summary = "최근 로그 조회", description = "최근 10개의 크롤링 로그를 조회합니다")
    public ResponseEntity<List<CrawlLog>> getRecentLogs(@PathVariable Long configId) {
        return ResponseEntity.ok(crawlLogService.getRecentLogsByConfigId(configId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "상태별 로그 조회", description = "상태별 크롤링 로그를 조회합니다")
    public ResponseEntity<Page<CrawlLog>> getLogsByStatus(
            @PathVariable CrawlLog.CrawlStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("startedAt").descending());
        return ResponseEntity.ok(crawlLogService.getLogsByStatus(status, pageRequest));
    }
}
