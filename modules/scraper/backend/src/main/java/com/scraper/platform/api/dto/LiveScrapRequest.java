package com.scraper.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 실시간 검색결과 스크랩 요청. 검색 응답의 job 맵 값을 그대로 전달한다.
 */
@Schema(description = "실시간 검색결과 스크랩 요청")
public record LiveScrapRequest(
        @Schema(description = "사이트명") @NotBlank String siteName,
        @Schema(description = "공고 URL") @NotBlank String url,
        @Schema(description = "회사명") @NotBlank String company,
        @Schema(description = "포지션") @NotBlank String position,
        @Schema(description = "경력") String career,
        @Schema(description = "기술 스택") String tech,
        @Schema(description = "지역") String location,
        @Schema(description = "마감일") String deadline
) {}
