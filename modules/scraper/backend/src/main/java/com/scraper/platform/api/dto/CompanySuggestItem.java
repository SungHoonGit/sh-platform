package com.scraper.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "수집 회사 자동완성 항목 (회사 추가 모달용)")
public record CompanySuggestItem(
        @Schema(description = "회사명(원문)")
        String companyName,

        @Schema(description = "정규화 회사명 (매칭 키)")
        String normalized,

        @Schema(description = "내 메모 존재 여부")
        boolean hasNote,

        @Schema(description = "메모 ID (있으면 상세 바로 열기)")
        Long noteId,

        @Schema(description = "차단 여부")
        boolean blocked
) {
}
