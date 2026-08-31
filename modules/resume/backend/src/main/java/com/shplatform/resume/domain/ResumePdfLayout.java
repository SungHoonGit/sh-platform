package com.shplatform.resume.domain;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.shplatform.resume.api.dto.ResumeViewResponse;
import java.util.List;

/**
 * 이력서 PDF 테마별 레이아웃 전략.
 * OpenPDF {@link Document}에 전체 페이지(헤더 + 섹션)를 렌더링한다.
 *
 * <p>구현체: {@link ClassicPdfLayout} — 단일 컬럼 + 굵은 가이드 라인,
 * {@link ModernPdfLayout} — 좌측 다크 사이드바 + teal 액센트,
 * {@link SaraminPdfLayout} — 사람인 스타일 박스 섹션 + 연락처 테이블.
 * {@link ResumePdfServiceImpl}이 문서의 {@code templateCode}로 선택한다.
 */
public interface ResumePdfLayout {

    /**
     * 페이지 렌더를 실행한다.
     *
     * @param document    열려 있는 대상 문서
     * @param view        이력서 전체 뷰
     * @param sectionKeys 문서 편성(included + order)에 따라 정렬된 섹션 key 목록
     * @param userId      소유 사용자 ID (프로필 사진 로드용)
     */
    void render(Document document, ResumeViewResponse view, List<String> sectionKeys, Long userId)
            throws DocumentException;
}