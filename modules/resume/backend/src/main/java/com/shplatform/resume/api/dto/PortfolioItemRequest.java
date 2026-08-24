package com.shplatform.resume.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 포트폴리오 작업물 등록/수정 요청.
 * FILE 타입은 사전에 POST /api/v1/files 로 업로드 후 반환된 storedPath(또는 fileId 참조)를 전달한다.
 *
 * @param title        작업물 제목 (필수)
 * @param itemType     유형 (필수, FILE 또는 LINK)
 * @param filePath     저장된 파일 경로 (FILE 타입 — 파일 업로드 API가 반환한 경로)
 * @param linkUrl      외부 URL (LINK 타입)
 * @param description  설명
 * @param displayOrder 표시 순서
 */
public record PortfolioItemRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Pattern(regexp = "FILE|LINK") String itemType,
        @Size(max = 300) String filePath,
        @Size(max = 300) String linkUrl,
        @Size(max = 500) String description,
        Integer displayOrder
) {
}
