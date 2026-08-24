package com.shplatform.resume.api.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 전체 이력 조립 응답 (이력서 뷰용).
 *
 * @param profile      인적사항 (미등록 시 null)
 * @param careers      경력 목록
 * @param educations   학력 목록
 * @param skills       스킬 목록
 * @param certificates 자격증 목록
 * @param projects     프로젝트 목록
 * @param introductions 자기소개 항목 목록
 * @param portfolioItems 포트폴리오 작업물 목록
 * @param generatedAt  조립 시각
 */
public record ResumeViewResponse(
        ProfileResponse profile,
        List<CareerResponse> careers,
        List<EducationResponse> educations,
        List<SkillResponse> skills,
        List<CertificateResponse> certificates,
        List<ProjectResponse> projects,
        List<IntroductionResponse> introductions,
        List<PortfolioItemResponse> portfolioItems,
        LocalDateTime generatedAt
) {
}
