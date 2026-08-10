package com.scraper.platform.controller;

import com.scraper.platform.model.CompanyRating;
import com.scraper.platform.service.CompanyRatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 기업 평점 API 컨트롤러.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/company-ratings")
@RequiredArgsConstructor
@Tag(name = "Company Rating", description = "기업 평점 조회 API")
public class CompanyRatingController {

    private final CompanyRatingService companyRatingService;

    /**
     * 여러 기업의 평점을 조회한다.
     * GET /api/v1/company-ratings?companyNames=삼성전자,네이버,카카오
     */
    @GetMapping
    @Operation(summary = "기업 평점 조회", description = "여러 기업의 평점을 조회합니다. 캐시된 데이터가 즉시 반환되며, 없는 기업은 Background에서 수집됩니다.")
    public ResponseEntity<Map<String, Object>> getRatings(
            @Parameter(description = "조회할 기업명 목록 (콤마 구분)")
            @RequestParam List<String> companyNames) {

        List<CompanyRating> ratings = companyRatingService.getRatings(companyNames);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ratings", ratings);
        response.put("total", ratings.size());
        response.put("cachedCount", ratings.stream()
                .filter(r -> r.getAverageScore() != null)
                .count());

        return ResponseEntity.ok(response);
    }

    /**
     * 단일 기업의 평점을 조회한다.
     * GET /api/v1/company-ratings/{companyName}
     */
    @GetMapping("/{companyName}")
    @Operation(summary = "단일 기업 평점 조회", description = "특정 기업의 평점을 조회합니다.")
    public ResponseEntity<CompanyRating> getRating(
            @Parameter(description = "기업명")
            @PathVariable String companyName) {

        CompanyRating rating = companyRatingService.getRating(companyName);
        if (rating == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rating);
    }

    /**
     * 기업 평점을 수동으로 수집한다.
     * POST /api/v1/company-ratings/collect
     */
    @PostMapping("/collect")
    @Operation(summary = "기업 평점 수동 수집", description = "지정된 기업들의 평점을 수동으로 수집합니다.")
    public ResponseEntity<Map<String, Object>> collectRatings(
            @RequestBody List<String> companyNames) {

        companyRatingService.scrapeAndCacheBatch(companyNames);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "기업 평점 수집이 시작되었습니다.");
        response.put("companyNames", companyNames);
        response.put("total", companyNames.size());

        return ResponseEntity.accepted().body(response);
    }

    /**
     * 캐시를 초기화한다.
     * DELETE /api/v1/company-ratings/cache
     */
    @DeleteMapping("/cache")
    @Operation(summary = "캐시 초기화", description = "기업 평점 캐시를 초기화합니다.")
    public ResponseEntity<Map<String, Object>> clearCache() {
        companyRatingService.clearCache();
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "캐시가 초기화되었습니다.");
        
        return ResponseEntity.ok(response);
    }
}
