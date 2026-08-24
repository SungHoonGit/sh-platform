package com.scraper.platform.api.dto;

import java.time.LocalDateTime;

/**
 * 공고 스크랩 응답. 스크랩 메타데이터와 원본 공고 정보를 함께 담는다.
 *
 * @param id        스크랩 ID
 * @param postingId 공고 ID
 * @param siteName  수집 사이트
 * @param company   회사명
 * @param position  직무/공고 제목
 * @param url       공고 URL
 * @param career    경력 요건
 * @param tech      기술 스택
 * @param location  지역
 * @param deadline  마감일
 * @param scrappedAt 스크랩 시각
 */
public record JobScrapResponse(
        Long id,
        Long postingId,
        String siteName,
        String company,
        String position,
        String url,
        String career,
        String tech,
        String location,
        String deadline,
        LocalDateTime scrappedAt
) {
}
