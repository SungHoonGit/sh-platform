package com.scraper.platform.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "회사 메모 목록 항목 (note_md 제외, 차단·평점 조인)")
public record CompanyNoteResponse(
        @Schema(description = "메모 ID (차단 전용 행이면 null)")
        Long id,

        @Schema(description = "화면 표시용 회사명")
        String companyNameDisplay,

        @Schema(description = "정규화 회사명 (차단 키워드)")
        String companyNameNormalized,

        @Schema(description = "내 별점 1~5 (null이면 미지정)")
        Integer myStars,

        @Schema(description = "북마크 여부")
        boolean bookmarked,

        @Schema(description = "차단 여부")
        boolean blocked,

        @Schema(description = "분석 메모 존재 여부")
        boolean hasNote,

        @Schema(description = "크롤링 평균 평점 (없으면 null)")
        Double averageScore,

        @Schema(description = "출처별 점수 (잡플래닛/잡코리아/사람인, 없으면 null)")
        Double jobplanetScore,

        Double jobkoreaScore,

        Double saraminScore,

        @Schema(description = "메모 수정 일시 (메모 없으면 null)")
        LocalDateTime updatedAt,

        @Schema(description = "관련 저장 공고 수 (차단행은 숨김 수로 표시)")
        Long hiddenCount,

        @Schema(description = "메모 태그 목록 (메모 없으면 빈 목록)")
        List<NoteCategoryResponse> categories
) {
}
