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

    /**
     * (질의형) PDF 다운로드 파일명을 결정한다. 형식은 {@code "(테마명) 문서제목.pdf"}.
     *
     * <p>테마명은 templateCode 기준 한글 라벨(클래식/모던/사람인형)이고, 문서 제목은
     * documentId에 해당하는 문서의 title을 사용한다. documentId가 없거나 문서를 찾지
     * 못하면 CLASSIC·"이력서" 기본값을 쓴다.
     *
     * @param userId     소유자 사용자 ID
     * @param documentId 문서 ID (null이면 기본값)
     * @return 다운로드 파일명 (예: "(모던) 2026 포트폴리오 이력서.pdf")
     */
    String pdfFilename(Long userId, Long documentId);
}