package com.scraper.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회사 메모 생성/수정 요청")
public record CompanyNoteRequest(
        @Schema(description = "회사명 (원문, 정규화해 멱등 저장)", example = "삼성전자")
        String companyName,

        @Schema(description = "내 별점 1~5 (null이면 미지정)", example = "4")
        Integer myStars,

        @Schema(description = "북마크 여부", example = "true")
        Boolean isBookmarked,

        @Schema(description = "분석 마크다운 원문", example = "## 총평\n- 복지 좋음")
        String noteMd
) {
}
