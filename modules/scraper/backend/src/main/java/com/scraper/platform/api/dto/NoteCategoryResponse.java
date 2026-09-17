package com.scraper.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회사 메모 태그 (block_reasons 마스터 공유)")
public record NoteCategoryResponse(
        @Schema(description = "카테고리 ID")
        Long id,

        @Schema(description = "카테고리명")
        String name
) {
}
