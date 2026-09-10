package com.shplatform.resume.domain;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.shplatform.resume.api.dto.CareerResponse;
import com.shplatform.resume.api.dto.DocumentResponse;
import com.shplatform.resume.api.dto.IntroductionResponse;
import com.shplatform.resume.api.dto.ProfileResponse;
import com.shplatform.resume.api.dto.ProjectResponse;
import com.shplatform.resume.api.dto.ResumeViewResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * SARAMIN 긴 섹션(자기소개) 페이지 분할, 문서 편성(숨김 섹션) 반영을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ResumePdfBlankPageTest {

    private static final Long USER_ID = 1L;
    private static final Long DOCUMENT_ID = 100L;

    @Mock
    private ResumeViewService resumeViewService;
    @Mock
    private ResumeDocumentService resumeDocumentService;
    @Mock
    private FileStorageService fileStorageService;

    private ResumePdfService service;

    @BeforeEach
    void setUp() {
        service = new ResumePdfServiceImpl(
                resumeViewService, resumeDocumentService,
                new ClassicPdfLayout(fileStorageService),
                new ModernPdfLayout(fileStorageService),
                new SaraminPdfLayout(fileStorageService));
    }

    @Test
    @DisplayName("SARAMIN: 자기소개가 길어 페이지를 넘어도 빈 페이지 없이 분할된다")
    void saramin_longIntroduction_hasNoBlankPage() throws Exception {
        given(resumeViewService.getMyResumeView(USER_ID))
                .willReturn(view("자기소개 " + "내용입니다. ".repeat(3_000)));
        given(resumeDocumentService.getDocuments(USER_ID))
                .willReturn(List.of(doc("긴 문서", "SARAMIN", null)));

        byte[] pdf = service.generatePdf(USER_ID, DOCUMENT_ID);

        assertThat(blankPageCount(pdf)).as("빈 페이지가 없어야 한다").isZero();
    }

    @Test
    @DisplayName("SARAMIN: 프로젝트 섹션을 숨긴 문서는 프로젝트 내용이 PDF에 렌더링되지 않는다")
    void saramin_hiddenProjects_areNotRendered() throws Exception {
        List<ProjectResponse> hiddenProjects = List.of(
                new ProjectResponse(3L, "숨김프로젝트1", "역할", LocalDate.of(2024, 1, 1), null, "설명",
                        "Java", null, null, null, null, null, 0, null, null),
                new ProjectResponse(4L, "숨김프로젝트2", "역할", LocalDate.of(2024, 2, 1), null, "설명",
                        "Spring", null, null, null, null, null, 1, null, null));
        given(resumeViewService.getMyResumeView(USER_ID))
                .willReturn(viewWithProjects("자기소개", hiddenProjects));
        given(resumeDocumentService.getDocuments(USER_ID))
                .willReturn(List.of(doc("숨김문서", "SARAMIN", """
                        [
                          {"key":"careers","included":true,"order":1},
                          {"key":"projects","included":false,"order":2},
                          {"key":"educations","included":false,"order":3},
                          {"key":"skills","included":false,"order":4},
                          {"key":"certificates","included":false,"order":5},
                          {"key":"introductions","included":true,"order":6},
                          {"key":"portfolioItems","included":false,"order":7}
                        ]""")));

        byte[] pdf = service.generatePdf(USER_ID, DOCUMENT_ID);

        String text = extract(pdf);
        assertThat(text).doesNotContain("숨김프로젝트");
        assertThat(text).contains("자기소개");
        assertThat(text).contains("회사명");
    }

    @Test
    @DisplayName("documentId 없으면 SARAMIN config를 참조할 수 없어 CLASSIC 기본 편성으로 폴백한다")
    void saramin_withoutDocumentId_fallsBackToClassicDefault() throws Exception {
        given(resumeViewService.getMyResumeView(USER_ID)).willReturn(view("자기소개"));

        byte[] pdf = service.generatePdf(USER_ID, null);

        String text = extract(pdf);
        assertThat(text).contains("자기소개");
        assertThat(text).contains("경력");
    }

    private ResumeViewResponse view(String introContent) {
        return viewWithProjects(introContent, List.of());
    }

    private ResumeViewResponse viewWithProjects(String introContent, List<ProjectResponse> projects) {
        List<CareerResponse> careers = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            careers.add(new CareerResponse((long) i, "회사" + i, "백엔드",
                    LocalDate.of(2020, 1, 1), null,
                    "경력 설명 입니다. ".repeat(60).trim(), List.of(), i, null, null));
        }
        return new ResumeViewResponse(
                new ProfileResponse(1L, "홍길동", "t@e.com", "010-1", "대전", LocalDate.of(1996, 1, 1), null,
                        "백엔드", null, null),
                careers, List.of(), List.of(), List.of(), projects,
                List.of(new IntroductionResponse(2L, "자기소개", introContent, 0, null, null)),
                List.of(), null);
    }

    private DocumentResponse doc(String title, String template, String config) {
        return new DocumentResponse(DOCUMENT_ID, title, template, true, 1, config, null, null);
    }

    private int blankPageCount(byte[] pdf) throws Exception {
        int blank = 0;
        try (PdfReader reader = new PdfReader(pdf)) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                String text = extractor.getTextFromPage(page);
                if (text == null || text.isBlank()) {
                    blank++;
                }
            }
        }
        return blank;
    }

    private String extract(byte[] pdf) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (PdfReader reader = new PdfReader(pdf)) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                sb.append(extractor.getTextFromPage(page));
            }
        }
        return sb.toString();
    }
}