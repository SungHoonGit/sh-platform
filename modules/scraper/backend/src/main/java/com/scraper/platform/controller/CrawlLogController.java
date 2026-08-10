package com.scraper.platform.controller;

import com.scraper.platform.api.dto.CrawlLogGroupResponse;
import com.scraper.platform.model.CrawlLog;
import com.scraper.platform.service.CrawlConfigService;
import com.scraper.platform.service.CrawlLogService;
import com.shplatform.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/crawl-logs")
@RequiredArgsConstructor
@Tag(name = "CrawlLog", description = "크롤링 로그 API")
public class CrawlLogController {

    private final CrawlLogService crawlLogService;
    private final CrawlConfigService crawlConfigService;

    @GetMapping("/config/{configId}")
    @Operation(summary = "설정별 로그 조회", description = "특정 설정의 크롤링 로그를 페이징하여 조회합니다")
    public ResponseEntity<Page<CrawlLog>> getLogsByConfigId(
            @PathVariable Long configId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        checkOwnership(configId);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("startedAt").descending());
        return ResponseEntity.ok(crawlLogService.getLogsByConfigId(configId, pageRequest));
    }

    @GetMapping("/config/{configId}/recent")
    @Operation(summary = "최근 로그 조회", description = "최근 10개의 크롤링 로그를 조회합니다")
    public ResponseEntity<List<CrawlLog>> getRecentLogs(@PathVariable Long configId) {
        checkOwnership(configId);
        return ResponseEntity.ok(crawlLogService.getRecentLogsByConfigId(configId));
    }

    @GetMapping("/config/{configId}/grouped")
    @Operation(summary = "날짜별 그룹 로그", description = "크롤링 로그를 날짜별로 그룹핑하여 조회합니다")
    public ResponseEntity<List<CrawlLogGroupResponse>> getLogsGroupedByDate(
            @PathVariable Long configId,
            @RequestParam(defaultValue = "30") int days) {
        checkOwnership(configId);
        return ResponseEntity.ok(crawlLogService.getLogsGroupedByDate(configId, days));
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

    @DeleteMapping("/{logId}")
    @Operation(summary = "크롤링 로그 삭제", description = "크롤링 로그와 관련 데이터를 삭제합니다")
    public ResponseEntity<Map<String, Object>> deleteLog(@PathVariable Long logId) {
        crawlLogService.deleteLog(logId);
        return ResponseEntity.ok(Map.of("success", true, "message", "삭제 완료"));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "크롤링 로그 일괄 삭제", description = "여러 크롤링 로그를 일괄 삭제합니다")
    public ResponseEntity<Map<String, Object>> deleteLogs(@RequestBody List<Long> logIds) {
        int deleted = crawlLogService.deleteLogs(logIds);
        return ResponseEntity.ok(Map.of("success", true, "deleted", deleted, "message", deleted + "건 삭제 완료"));
    }

    private void checkOwnership(Long configId) {
        crawlConfigService.getConfigById(configId, SecurityUtils.currentAccountId());
    }
}
