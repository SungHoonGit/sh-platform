package com.scraper.platform.api.dto;

import java.time.LocalDate;

/**
 * 채용공고 요약 정보.
 *
 * @param id        공고 ID
 * @param siteName  수집 사이트
 * @param company   회사명
 * @param position  직무/공고 제목
 * @param career    경력 요건
 * @param tech      기술 스택
 * @param location  지역
 * @param deadline  마감일
 * @param url       공고 URL
 * @param crawledAt 수집일
 */
public record JobPostingSummary(
        Long id,
        String siteName,
        String company,
        String position,
        String career,
        String tech,
        String location,
        String deadline,
        String url,
        LocalDate crawledAt
) {
}
