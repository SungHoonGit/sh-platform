package com.scraper.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "실시간 크롤링 검색 요청")
public record CrawlSearchRequest(
    @Schema(description = "검색 키워드", example = "Java")
    String keyword,
    @Schema(description = "경력 조건", example = "3~5년")
    String career,
    @Schema(description = "지역 조건", example = "서울")
    String location,
    @Schema(description = "검색할 사이트 ID 목록", example = "[\"saramin\", \"jobkorea\"]")
    List<String> siteIds
) {}
