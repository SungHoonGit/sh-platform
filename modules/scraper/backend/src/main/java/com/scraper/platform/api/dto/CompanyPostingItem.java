package com.scraper.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "회사 관련 저장 공고 (슬라이드 보기 탭용, 최근순)")
public record CompanyPostingItem(
        @Schema(description = "포지션/제목")
        String position,

        @Schema(description = "사이트명")
        String siteName,

        @Schema(description = "회사명(원문)")
        String company,

        @Schema(description = "수집일")
        LocalDate crawledAt,

        @Schema(description = "공고 URL")
        String url
) {
}
