package com.scraper.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "실시간 검색 요청")
public record SearchRequest(
    @Schema(description = "검색 키워드", example = "Java")
    String keyword,

    @Schema(description = "경력 조건", example = "3~5년")
    String career,

    @Schema(description = "지역", example = "서울")
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
