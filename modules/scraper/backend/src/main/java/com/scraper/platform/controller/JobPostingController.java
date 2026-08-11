package com.scraper.platform.controller;

import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            @RequestParam(required = false) String runIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortKey,
            @RequestParam(required = false) String sortOrder
    ) {
        Sort sort;
        if (sortKey != null && !sortKey.isEmpty()) {
            Sort.Direction dir = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
            switch (sortKey) {
                case "company" -> sort = Sort.by(dir, "company");
                case "position" -> sort = Sort.by(dir, "position");
                case "career" -> sort = Sort.by(dir, "career");
                case "location" -> sort = Sort.by(dir, "location");
                case "tech" -> sort = Sort.by(dir, "tech");
                case "deadline" -> sort = Sort.by(dir, "deadline");
                case "site" -> sort = Sort.by(dir, "siteName");
                default -> sort = Sort.by(Sort.Direction.DESC, "crawledAt").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            }
        } else {
            sort = Sort.by(Sort.Direction.DESC, "crawledAt").and(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        PageRequest pageRequest = PageRequest.of(page, size, sort);

        LocalDate date = null;
        if (crawledAt != null && !crawledAt.isEmpty()) {
            try {
                date = LocalDate.parse(crawledAt);
            } catch (Exception e) {
            }
        }

        List<Long> runIdList = null;
        if (runIds != null && !runIds.isEmpty()) {
            try {
                runIdList = Arrays.stream(runIds.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
            } catch (Exception e) {
            }
        }

        Page<JobPosting> postings;
        if (runIdList != null && !runIdList.isEmpty()) {
            if (siteName != null && !siteName.isEmpty() && !"all".equals(siteName)) {
                postings = jobPostingRepository.findByConfigIdAndSiteNameAndCrawlLogIdIn(configId, siteName, runIdList, pageRequest);
            } else {
                postings = jobPostingRepository.findByConfigIdAndCrawlLogIdIn(configId, runIdList, pageRequest);
            }
        } else if (date != null && siteName != null && !siteName.isEmpty() && !"all".equals(siteName)) {
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
    @Operation(summary = "채용공고 엑셀 내보내기 (사이트별 시트 분리)")
    public void exportExcel(
            @RequestParam Long configId,
            @RequestParam(required = false) String siteName,
            @RequestParam(required = false) String crawledAt,
            @RequestParam(required = false) String runIds,
            HttpServletResponse response
    ) throws IOException {
        LocalDate date = null;
        if (crawledAt != null && !crawledAt.isEmpty()) {
            try {
                date = LocalDate.parse(crawledAt);
            } catch (Exception e) {
            }
        }

        List<Long> runIdList = null;
        if (runIds != null && !runIds.isEmpty()) {
            try {
                runIdList = Arrays.stream(runIds.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
            } catch (Exception e) {
            }
        }

        Sort exportSort = Sort.by(Sort.Direction.DESC, "crawledAt").and(Sort.by(Sort.Direction.DESC, "createdAt"));

        // 전체 시트용: 항상 모든 데이터 조회
        List<JobPosting> allPostings;
        if (runIdList != null && !runIdList.isEmpty()) {
            allPostings = jobPostingRepository.findByConfigIdAndCrawlLogIdIn(configId, runIdList, exportSort);
        } else if (date != null) {
            allPostings = jobPostingRepository.findByConfigIdAndCrawledAt(configId, date, exportSort);
        } else {
            allPostings = jobPostingRepository.findByConfigId(configId, exportSort);
        }

        // 사이트별 시트용: 선택된 사이트만 필터
        List<JobPosting> postings;
        if (runIdList != null && !runIdList.isEmpty()) {
            if (siteName != null && !siteName.isEmpty() && !"all".equals(siteName)) {
                postings = jobPostingRepository.findByConfigIdAndSiteNameAndCrawlLogIdIn(configId, siteName, runIdList, exportSort);
            } else {
                postings = allPostings;
            }
        } else if (date != null && siteName != null && !siteName.isEmpty() && !"all".equals(siteName)) {
            postings = jobPostingRepository.findByConfigIdAndSiteNameAndCrawledAt(configId, siteName, date, exportSort);
        } else if (date != null) {
            postings = allPostings;
        } else if (siteName != null && !siteName.isEmpty() && !"all".equals(siteName)) {
            postings = jobPostingRepository.findByConfigIdAndSiteName(configId, siteName, exportSort);
        } else {
            postings = allPostings;
        }

        Map<String, List<JobPosting>> bySite = new LinkedHashMap<>();
        for (JobPosting p : postings) {
            String site = p.getSiteName() != null ? p.getSiteName() : "기타";
            bySite.computeIfAbsent(site, k -> new java.util.ArrayList<>()).add(p);
        }

        Map<String, String> siteNameMap = Map.of(
            "saramin", "사람인",
            "jobkorea", "잡코리아",
            "wanted", "원티드",
            "remember", "리멤버"
        );

        String fileName = URLEncoder.encode("채용공고_" + LocalDate.now(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        try (ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), JobPostingVO.class).build()) {
            int sheetIndex = 0;

            // 전체 시트 먼저 추가
            List<JobPostingVO> allVoList = allPostings.stream()
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
            WriteSheet allSheet = EasyExcel.writerSheet(sheetIndex, "전체").build();
            excelWriter.write(allVoList, allSheet);
            sheetIndex++;

            // 사이트별 시트
            for (Map.Entry<String, List<JobPosting>> entry : bySite.entrySet()) {
                String siteKey = entry.getKey();
                List<JobPosting> sitePostings = entry.getValue();
                String sheetName = siteNameMap.getOrDefault(siteKey, siteKey);

                List<JobPostingVO> voList = sitePostings.stream()
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

                WriteSheet sheet = EasyExcel.writerSheet(sheetIndex, sheetName).build();
                excelWriter.write(voList, sheet);
                sheetIndex++;
            }
        }
    }
}
