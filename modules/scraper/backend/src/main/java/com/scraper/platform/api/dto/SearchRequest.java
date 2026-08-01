package com.scraper.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "실시간 검색 요청")
public record SearchRequest(
    @Schema(description = "검색 키워드", example = "Java")
    String keyword,

    @Schema(description = "경력 최소 연수 (0~15, null이면 하한 없음)", example = "2")
    Integer careerMin,

    @Schema(description = "경력 최대 연수 (null이면 상한 없음)", example = "7")
    Integer careerMax,

    @Schema(description = "지역 목록 (국토부 기준 17개 시/도)", example = "[\"서울\",\"경기\"]")
    List<String> locations,

    @Schema(description = "경력 조건 (레거시 단일 값, \"3~5년\" 형식)")
    String career,

    @Schema(description = "지역 (레거시 단일 값)")
    String location,

    @Schema(description = "검색할 사이트 목록", example = "[\"saramin\",\"jobkorea\",\"wanted\",\"remember\"]")
    List<String> sites
) {
    public SearchRequest {
        if (sites == null || sites.isEmpty()) {
            sites = List.of("saramin", "jobkorea", "wanted", "remember");
        }
    }
}
