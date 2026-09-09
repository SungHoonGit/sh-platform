package com.shplatform.resume.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 포트폴리오 작업물 등록/수정 요청.
 * FILE 타입은 사전에 POST /api/v1/files 로 업로드 후 반환된 storedPath(또는 fileId 참조)를 전달한다.
 *
 * @param title         작업물 제목 (필수)
 * @param itemType      유형 (비면 자동 결정 — 첨부파일 있으면 FILE, 없으면 LINK)
 * @param thumbnailPath 썸네일 이미지 저장 경로 (선택)
 * @param githubUrl     GitHub 저장소 링크 (선택)
 * @param demoUrl       데모/배포 링크 (선택)
 * @param videoUrl      시연 영상 링크 (선택)
 * @param filePath      저장된 파일 경로 (첨부 — 파일 업로드 API가 반환한 경로)
 * @param linkUrl       외부 URL (호환용, 레거시 LINK 타입)
 * @param description   설명
 * @param displayOrder  표시 순서
 */
public record PortfolioItemRequest(
        @NotBlank @Size(max = 100) String title,
        @Pattern(regexp = "FILE|LINK") String itemType,
        @Size(max = 300) String thumbnailPath,
        @Size(max = 300) String githubUrl,
        @Size(max = 300) String demoUrl,
        @Size(max = 300) String videoUrl,
        @Size(max = 300) String filePath,
        @Size(max = 300) String linkUrl,
        @Size(max = 500) String description,
        Integer displayOrder
) {
}
