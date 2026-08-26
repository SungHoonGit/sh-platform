package com.scraper.platform.controller;

import com.scraper.platform.api.dto.JobScrapResponse;
import com.scraper.platform.service.JobScrapService;
import com.shplatform.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 공고 스크랩(북마크) API 컨트롤러.
 */
@RestController
@RequestMapping("/job-scrap")
@RequiredArgsConstructor
@Tag(name = "Job Scrap", description = "공고 스크랩(북마크) API")
public class JobScrapController {

    private final JobScrapService jobScrapService;

    /**
     * 내 스크랩 목록을 조회한다.
     * GET /scraper/api/v1/job-scrap
     */
    @GetMapping
    @Operation(summary = "내 스크랩 목록", description = "로그인 사용자가 스크랩한 공고 목록을 최신순으로 반환합니다.")
    public ResponseEntity<Map<String, Object>> getMyScraps() {
        List<JobScrapResponse> scraps = jobScrapService.getMyScraps(SecurityUtils.currentAccountId());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("scraps", scraps);
        response.put("total", scraps.size());
        return ResponseEntity.ok(response);
    }

    /**
     * 실시간 검색결과를 저장하고 스크랩한다.
     * POST /scraper/api/v1/job-scrap/live
     */
    @PostMapping("/live")
    @Operation(summary = "실시간 검색결과 스크랩", description = "검색 결과를 DB에 저장(ID 부여)한 뒤 스크랩합니다.")
    public ResponseEntity<java.util.Map<String, Object>> scrapLive(
            @RequestBody com.scraper.platform.api.dto.LiveScrapRequest request) {
        Long postingId = jobScrapService.scrapLive(SecurityUtils.currentAccountId(), request);
        java.util.Map<String, Object> response = new LinkedHashMap<>();
        response.put("postingId", postingId);
        response.put("scrapped", true);
        return ResponseEntity.ok(response);
    }

    /**
     * 공고를 스크랩한다.
     * POST /scraper/api/v1/job-scrap/{postingId}
     */
    @PostMapping("/{postingId}")
    @Operation(summary = "공고 스크랩", description = "공고를 저장합니다. 이미 스크랩된 경우 무시됩니다.")
    public ResponseEntity<Void> scrap(@PathVariable Long postingId) {
        jobScrapService.scrap(SecurityUtils.currentAccountId(), postingId);
        return ResponseEntity.ok().build();
    }

    /**
     * 스크랩을 해제한다.
     * DELETE /api/v1/job-scrap/{postingId}
     */
    @DeleteMapping("/{postingId}")
    @Operation(summary = "스크랩 해제")
    public ResponseEntity<Void> unscrap(@PathVariable Long postingId) {
        jobScrapService.unscrap(SecurityUtils.currentAccountId(), postingId);
        return ResponseEntity.ok().build();
    }

    /**
     * 공고의 스크랩 여부를 확인한다.
     * GET /api/v1/job-scrap/{postingId}
     */
    @GetMapping("/{postingId}")
    @Operation(summary = "스크랩 여부 확인")
    public ResponseEntity<Map<String, Object>> isScrapped(@PathVariable Long postingId) {
        boolean scrapped = jobScrapService.isScrapped(SecurityUtils.currentAccountId(), postingId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("postingId", postingId);
        response.put("scrapped", scrapped);
        return ResponseEntity.ok(response);
    }
}
