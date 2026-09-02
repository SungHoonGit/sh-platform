package com.shplatform.resume.api.dto;

/**
 * 공개 공유 이력서 뷰 응답.
 * 소유자가 보는 것과 동일하게 문서(템플릿/섹션 편성)를 적용할 수 있도록 문서 메타데이터와 함께 반환한다.
 *
 * @param documentId    공유 문서 ID
 * @param title         문서 제목
 * @param templateCode  템플릿 코드 (CLASSIC/MODERN/SARAMIN)
 * @param sectionConfig 섹션 편성 JSON
 * @param view          조립된 이력서 뷰 (전 항목)
 */
public record ShareViewResponse(
        Long documentId,
        String title,
        String templateCode,
        String sectionConfig,
        ResumeViewResponse view
) {
}