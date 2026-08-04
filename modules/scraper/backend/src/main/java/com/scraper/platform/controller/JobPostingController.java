package com.scraper.platform.controller;

import com.scraper.platform.api.dto.JobPostingResponse;
import com.scraper.platform.model.JobPosting;
import com.scraper.platform.repository.JobPostingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 채용공고 Viewer API.
 * DB 기반 데이터 조회.
 */
@RestController
@RequestMapping("/scraper/api/v1/job-postings")
@RequiredArgsConstructor
@Tag(name = "JobPostings", description = "채용공고 조회 API")
public class JobPostingController {

    private final JobPostingRepository jobPostingRepository;

    @GetMapping
    @Operation(summary = "채용공고 목록 조회 (페이지네이션)")
    public ResponseEntity<JobPostingResponse> getJobs(
            @RequestParam Long configId,
            @RequestParam(required = false) String siteName,
            @RequestParam(required = false) LocalDate crawledAt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "crawledAt").and(Sort.by(Sort.Direction.DESC, "createdAt")));

        // 동적 조건构建
        Specification<JobPosting> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("config").get("id"), configId));

            if (siteName != null && !siteName.isEmpty() && !"all".equals(siteName)) {
                predicates.add(cb.equal(root.get("siteName"), siteName));
            }
            if (crawledAt != null) {
                predicates.add(cb.equal(root.get("crawledAt"), crawledAt));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<JobPosting> postings = jobPostingRepository.findAll(spec, pageRequest);

        List<JobPostingResponse.JobItem> items = postings.getContent().stream()
                .map(p -> JobPostingResponse.JobItem.builder()
                        .id(p.getId())
                        .site(p.getSiteName())
                        .company(p.getCompany())
                        .position(p.getPosition())
                        .career(p.getCareer())
                        .tech(p.getTech())
                        .location(p.getLocation())
                        .deadline(p.getDeadline())
                        .url(p.getUrl())
                        .crawledAt(p.getCrawledAt())
                        .build())
                .toList();

        JobPostingResponse response = JobPostingResponse.builder()
                .jobs(items)
                .total((int) postings.getTotalElements())
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/dates")
    @Operation(summary = "수집된 날짜 목록 조회")
    public ResponseEntity<List<LocalDate>> getCrawledDates(@RequestParam Long configId) {
        List<LocalDate> dates = jobPostingRepository.findByConfigIdOrderByCrawledAtDescCreatedAtDesc(configId)
                .stream()
                .map(JobPosting::getCrawledAt)
                .distinct()
                .sorted(java.util.Comparator.reverseOrder())
                .toList();
        return ResponseEntity.ok(dates);
    }

    @GetMapping("/stats")
    @Operation(summary = "채용공고 통계")
    public ResponseEntity<?> getStats(@RequestParam Long configId) {
        long total = jobPostingRepository.countByConfigId(configId);
        LocalDate today = LocalDate.now();
        long todayCount = jobPostingRepository.countByConfigIdAndCrawledAt(configId, today);
        LocalDate lastCrawled = jobPostingRepository.findLastCrawledAt(configId);

        return ResponseEntity.ok(java.util.Map.of(
                "total", total,
                "todayCount", todayCount,
                "lastCrawledAt", lastCrawled != null ? lastCrawled.toString() : null
        ));
    }
}
