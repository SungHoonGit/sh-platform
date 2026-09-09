package com.shplatform.resume.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 프로젝트 등록/수정 요청.
 *
 * @param name         프로젝트명 (필수)
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
 */
public record ProjectRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String role,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        @Size(max = 300) String techStack,
        @Size(max = 300) String githubUrl,
        @Size(max = 300) String demoUrl,
        @Size(max = 300) String videoUrl,
        @Size(max = 300) String linkUrl,
        @Size(max = 300) String thumbnailPath,
        Integer displayOrder
) {
}
