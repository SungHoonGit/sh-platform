package com.shplatform.resume.api.dto;

/**
 * 이력서 문서 수정 요청. null 필드는 변경하지 않는다.
 *
 * @param title         문서 제목
 * @param templateCode  템플릿 코드
 * @param sectionConfig 섹션 편성 JSON 문자열 (형식 검증 대상)
 * @param primary       대표 문서 여부
 */
public record DocumentUpdateRequest(
        String title,
        String templateCode,
        String sectionConfig,
        Boolean primary
) {
}
