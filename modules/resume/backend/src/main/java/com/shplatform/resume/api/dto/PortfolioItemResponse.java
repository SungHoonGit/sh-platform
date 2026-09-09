package com.shplatform.resume.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 포트폴리오 작업물 응답.
 *
 * @param id           작업물 ID
 * @param title        프로젝트/작업물 제목
 * @param itemType     유형 (FILE/LINK)
 * @param role         담당 역할
 * @param startDate    시작일
 * @param endDate      종료일
 * @param techStack    기술 스택
 * @param thumbnailPath 썸네일 이미지 저장 경로
 * @param githubUrl    GitHub 저장소 링크
 * @param demoUrl      데모/배포 링크
 * @param videoUrl     시연 영상 링크
 * @param filePath     저장된 파일 경로 (첨부)
 * @param linkUrl      외부 URL (레거시)
 * @param description  설명
 * @param displayOrder 표시 순서
 * @param createdAt    등록 시각
 */
public record PortfolioItemResponse(
        Long id,
        String title,
        String itemType,
        String role,
        LocalDate startDate,
        LocalDate endDate,
        String techStack,
        String thumbnailPath,
        String githubUrl,
        String demoUrl,
        String videoUrl,
        String filePath,
        String linkUrl,
        String description,
        Integer displayOrder,
        LocalDateTime createdAt
) {
}