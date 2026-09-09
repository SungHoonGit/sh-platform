package com.shplatform.resume.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 프로젝트 응답.
 *
 * @param id           프로젝트 ID
 * @param name         프로젝트명
 * @param role         담당 역할
 * @param startDate    시작일
 * @param endDate      종료일
 * @param description  프로젝트 설명
 * @param techStack    기술 스택
 * @param githubUrl    GitHub 저장소 링크
 * @param demoUrl      데모/배포 링크
 * @param videoUrl     시연 영상 링크
 * @param linkUrl      관련 링크 (레거시 단일 링크)
 * @param thumbnailPath 썸네일 이미지 저장 경로
 * @param displayOrder 표시 순서
 * @param createdAt    등록 시각
 * @param updatedAt    수정 시각
 */
public record ProjectResponse(
        Long id,
        String name,
        String role,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        String techStack,
        String githubUrl,
        String demoUrl,
        String videoUrl,
        String linkUrl,
        String thumbnailPath,
        Integer displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
