package com.shplatform.resume.api.dto;

/**
 * 기술 스택 마스터 응답 (이력서 프로젝트/경력 기술 스택 자동완성용).
 *
 * @param id       기술 스택 ID
 * @param name     정식 기술명
 * @param category 분류 (language/framework/database/infra/tool/etc)
 */
public record SkillMasterResponse(Long id, String name, String category) {
}
