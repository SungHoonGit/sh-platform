package com.scraper.platform.controller;

import com.scraper.platform.api.dto.CrawlSearchRequest;
import com.scraper.platform.service.CrawlExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/crawl/search")
@RequiredArgsConstructor
@Tag(name = "CrawlSearch", description = "실시간 크롤링 검색 API")
public class CrawlSearchController {

    private final CrawlExecutionService crawlExecutionService;

    @PostMapping
    @Operation(summary = "실시간 검색", description = "키워드/경력/지역 조건으로 크롤러를 실시간 실행하고 결과를 반환합니다 (저장 없음)")
    public ResponseEntity<List<Map<String, Object>>> search(@RequestBody CrawlSearchRequest request) {
        List<Map<String, Object>> results = crawlExecutionService.searchSites(
                request.keyword(),
                request.career(),
                request.location(),
                request.siteIds());
        return ResponseEntity.ok(results);
    }
}
