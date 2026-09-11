package com.scraper.platform.controller;

import com.scraper.platform.api.dto.JobPostingExportItem;
import com.scraper.platform.api.dto.JobPostingExportResponse;
import com.scraper.platform.model.JobPosting;
import com.scraper.platform.repository.JobPostingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 채용공고 외부 연동 export API.
 * <p>RAG(AgentOS) 등 외부 시스템이 JWT 없이 API 키(X-API-Key 헤더)로 공고 데이터를
 * 내려받기 위한 전용 엔드포인트. 인증은 {@code ApiKeyFilter}가 담당한다.
 */
@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
@Tag(name = "JobPostingExport", description = "채용공고 외부 연동 export API")
public class JobPostingExportController {

    private final JobPostingRepository jobPostingRepository;

    @GetMapping("/job-postings")
    @Operation(summary = "공고 export (API 키 인증)", description = "키워드(company/position/tech 부분 일치)로 공고를 페이지네이션하여 반환합니다. Java 공고 분석 등 RAG 연동용.")
    public ResponseEntity<JobPostingExportResponse> exportJobPostings(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        int safeSize = Math.min(Math.max(size, 1), 500);
        PageRequest pageRequest = PageRequest.of(page, safeSize,
                Sort.by(Sort.Direction.DESC, "crawledAt")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt")));

        String kw = (keyword != null && keyword.isBlank()) ? null : keyword;
        Page<JobPosting> result = jobPostingRepository.searchExport(kw, pageRequest);

        var jobs = result.getContent().stream()
                .map(p -> new JobPostingExportItem(
                        p.getId(), p.getSiteName(), p.getCompany(), p.getPosition(),
                        p.getCareer(), p.getTech(), p.getLocation(), p.getDeadline(),
                        p.getUrl(), p.getCrawledAt()))
                .toList();

        return ResponseEntity.ok(new JobPostingExportResponse(
                jobs, result.getTotalElements(), page, safeSize));
    }
}