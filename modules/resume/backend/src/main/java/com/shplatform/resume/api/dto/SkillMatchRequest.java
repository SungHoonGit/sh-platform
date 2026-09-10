package com.shplatform.resume.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 공고-이력서 기술 매칭 요청.
 *
 * @param techStack 공고의 기술 스택 문자열 (콤마 구분, 예: "Java, Spring, React.js")
 */
public record SkillMatchRequest(
        @NotNull(message = "techStack은 필수입니다.") String techStack) {
}