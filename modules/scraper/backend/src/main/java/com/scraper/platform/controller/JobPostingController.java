package com.scraper.platform.controller;

import cn.idev.excel.EasyExcel;
import com.scraper.platform.api.dto.JobPostingResponse;
import com.scraper.platform.api.dto.JobPostingVO;
import com.scraper.platform.model.JobPosting;
import com.scraper.platform.repository.JobPostingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * 채용공고 Viewer API.
 * DB 기반 데이터 조회.
 */
@RestController
@RequestMapping("/job-postings")
@RequiredArgsConstructor
@Tag(name = "JobPostings", description = "채용공고 조회 API")
public class JobPostingController {

    private final JobPostingRepository jobPostingRepository;

    @GetMapping
    @Operation(summary = "채용공고 목록 조회 (페이지네이션)")
    public ResponseEntity<JobPostingResponse> getJobs(
            @RequestParam Long configId,
            @RequestParam(required = false) String siteName,
            @RequestParam(required = false) String crawledAt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, 
            Sort.by(Sort.Direction.DESC, "crawledAt").and(Sort.by(Sort.Direction.DESC, "createdAt")));

        LocalDate date = null;
        if (crawledAt != null && !crawledAt.isEmpty()) {
            try {
                date = LocalDate.parse(crawledAt);
            } catch (Exception e) {
                // invalid date, ignore
            }
        }

        Page<JobPosting> postings;
        if (date != null && siteName != null && !siteName.isEmpty() && !"all".equals(siteName)) {
            postings = jobPostingRepository.findByConfigIdAndSiteNameAndCrawledAt(configId, siteName, date, pageRequest);
        } else if (date != null) {
            postings = jobPostingRepository.findByConfigIdAndCrawledAt(configId, date, pageRequest);
        } else if (siteName != null && !siteName.isEmpty() && !"all".equals(siteName)) {
            postings = jobPostingRepository.findByConfigIdAndSiteName(configId, siteName, pageRequest);
        } else {
            postings = jobPostingRepository.findByConfigId(configId, pageRequest);
        }

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
        List<LocalDate> dates = jobPostingRepository.findDistinctDatesByConfigId(configId);
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

    @GetMapping("/export")
    @Operation(summary = "채용공고 엑셀 내보내기")
    public void exportExcel(
            @RequestParam Long configId,
            @RequestParam(required = false) String siteName,
            @RequestParam(required = false) String crawledAt,
            HttpServletResponse response
    ) throws IOException {
        LocalDate date = null;
        if (crawledAt != null && !crawledAt.isEmpty()) {
            try {
                date = LocalDate.parse(crawledAt);
            } catch (Exception e) {
                // invalid date, ignore
            }
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "crawledAt").and(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<JobPosting> postings;
        if (date != null && siteName != null && !siteName.isEmpty() && !"all".equals(siteName)) {
            postings = jobPostingRepository.findByConfigIdAndSiteNameAndCrawledAt(configId, siteName, date, sort);
        } else if (date != null) {
            postings = jobPostingRepository.findByConfigIdAndCrawledAt(configId, date, sort);
        } else if (siteName != null && !siteName.isEmpty() && !"all".equals(siteName)) {
            postings = jobPostingRepository.findByConfigIdAndSiteName(configId, siteName, sort);
        } else {
            postings = jobPostingRepository.findByConfigId(configId, sort);
        }

        List<JobPostingVO> voList = postings.stream()
                .map(p -> JobPostingVO.builder()
                        .siteName(p.getSiteName())
                        .company(p.getCompany())
                        .position(p.getPosition())
                        .career(p.getCareer())
                        .tech(p.getTech())
                        .location(p.getLocation())
                        .deadline(p.getDeadline())
                        .url(p.getUrl())
                        .crawledAt(p.getCrawledAt() != null ? p.getCrawledAt().toString() : "")
                        .build())
                .toList();

        String fileName = URLEncoder.encode("채용공고_" + LocalDate.now(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), JobPostingVO.class)
                .sheet("채용공고")
                .doWrite(voList);
    }
}
