package com.scraper.platform.controller;

import com.scraper.platform.model.CrawlData;
import com.scraper.platform.service.CrawlDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/crawl-data")
@RequiredArgsConstructor
@Tag(name = "CrawlData", description = "크롤링 데이터 관리 API")
public class CrawlDataController {

    private final CrawlDataService crawlDataService;

    @GetMapping("/category/{slug}")
    @Operation(summary = "카테고리별 데이터 조회", description = "카테고리별 크롤링 데이터를 페이징하여 조회합니다")
    public ResponseEntity<Page<CrawlData>> getCrawlDataByCategory(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(crawlDataService.getCrawlDataByCategory(slug, pageRequest));
    }

    @GetMapping("/search")
    @Operation(summary = "크롤링 데이터 검색", description = "키워드로 크롤링 데이터를 검색합니다")
    public ResponseEntity<Page<CrawlData>> searchCrawlData(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(crawlDataService.searchCrawlData(keyword, pageRequest));
    }

    @GetMapping("/category/{slug}/count")
    @Operation(summary = "카테고리별 데이터 수 조회", description = "카테고리별 크롤링 데이터 수를 조회합니다")
    public ResponseEntity<Long> getCountByCategory(@PathVariable String slug) {
        return ResponseEntity.ok(crawlDataService.getCountByCategory(slug));
    }
}
