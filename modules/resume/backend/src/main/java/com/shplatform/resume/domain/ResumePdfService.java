package com.shplatform.resume.domain;

/**
 * (명령형) 이력서를 서버사이드에서 A4 PDF로 생성하는 서비스.
 */
public interface ResumePdfService {

    /**
     * (질의형) 사용자의 기본 이력서를 A4 PDF로 렌더링하여 바이트 배열로 반환한다.
     *
     * <p>데이터는 {@link ResumeViewService#getMyResumeView(Long)} 조립 결과를 재사용하며,
     * 프로필 미등록·항목 전무여도 빈 섹션을 생략한 유효한 PDF를 생성한다.
     *
     * @param userId 소유자 사용자 ID
     * @return application/pdf 바이트 배열
     */
    byte[] generatePdf(Long userId);
}