package com.scraper.platform.api.dto;

import java.time.LocalDate;

/**
 * 채용공고 외부 연동(export)용 응답 아이템.
 * <p>RAG·외부 시스템 연동을 위해 필수 메타데이터만 노출한다.
 *
 * @param id         공고 ID
 * @param siteName   수집 사이트 (saramin/jobkorea/wanted/remember)
 * @param company    회사명
 * @param position   공고명/포지션
 * @param career     경력 요구 (예: 경력3년↑)
 * @param tech       기술 스택 (콤마 구분)
 * @param location   근무 지역
 * @param deadline   마감일 (문자열)
 * @param url        공고 원문 URL
 * @param crawledAt  수집 날짜
 */
public record JobPostingExportItem(
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