package com.scraper.platform.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 최근 수집 공고 목록 응답 (모듈 간 조회용).
 *
 * @param items 공고 요약 목록
 * @param total 전체 건수
 * @param page  페이지 번호
 * @param size  페이지 크기
 */
public record RecentPostingsResponse(
        List<JobPostingSummary> items,
        long total,
        int page,
        int size
) {
}
