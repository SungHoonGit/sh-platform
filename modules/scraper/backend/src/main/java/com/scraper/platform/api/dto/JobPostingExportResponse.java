package com.scraper.platform.api.dto;

import java.util.List;

/**
 * 채용공고 외부 연동 export 응답 (페이지네이션 포함).
 *
 * @param jobs    페이지 내 공고 목록
 * @param total   전체 공고 수
 * @param page    현재 페이지 (0-based)
 * @param size    페이지 크기
 */
public record JobPostingExportResponse(
        List<JobPostingExportItem> jobs,
        long total,
        int page,
        int size
) {
}