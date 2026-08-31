package com.shplatform.resume.domain;

/**
 * (명령형) 이력서를 서버사이드에서 A4 PDF로 생성하는 서비스.
 */
public interface ResumePdfService {

    /**
     * (질의형) 사용자의 이력서를 A4 PDF로 렌더링하여 바이트 배열로 반환한다.
     *
     * <p>데이터는 {@link ResumeViewService#getMyResumeView(Long)} 조립 결과를 재사용하며,
     * documentId가 지정되면 해당 문서의 섹션 편성(sectionConfig: 포함 여부·순서)을 반영한다.
     * 프로필 미등록·항목 전무여도 빈 섹션을 생략한 유효한 PDF를 생성한다.
     *
     * @param userId     소유자 사용자 ID
     * @param documentId 문서 ID (null이면 기본 편성 순서 사용)
     * @return application/pdf 바이트 배열
     */
    byte[] generatePdf(Long userId, Long documentId);
}