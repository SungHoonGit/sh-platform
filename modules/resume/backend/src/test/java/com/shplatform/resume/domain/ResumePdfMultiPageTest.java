package com.shplatform.resume.domain;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.shplatform.resume.api.dto.CareerResponse;
import com.shplatform.resume.api.dto.DocumentResponse;
import com.shplatform.resume.api.dto.EducationResponse;
import com.shplatform.resume.api.dto.ProfileResponse;
import com.shplatform.resume.api.dto.ResumeViewResponse;
import com.shplatform.resume.api.dto.SkillResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * 다중 페이지 생성 시 어떤 테마에서도 내용이 유실되지 않는지 검증한다.
 * 페이지 경계(테이블 분할/중첩)에서 클리핑이 발생하면 추출 텍스트에 항목이 누락된다.
 */
class ResumePdfMultiPageTest {

    private static final Long USER_ID = 1L;
    private static final Long DOCUMENT_ID = 100L;

    private final ResumeViewService resumeViewService = Mockito.mock(ResumeViewService.class);
    private final ResumeDocumentService resumeDocumentService = Mockito.mock(ResumeDocumentService.class);
    private final FileStorageService fileStorageService = Mockito.mock(FileStorageService.class);

    @Test
    @DisplayName("generatePdf: 모든 테마가 여러 페이지에 걸쳐도 항목을 잃지 않는다")
    void allThemesKeepEveryItemOnMultiPage() throws Exception {
        ResumeViewResponse view = multiPageView();
        given(resumeViewService.getMyResumeView(USER_ID)).willReturn(view);

        for (String theme : List.of("CLASSIC", "MODERN", "SARAMIN")) {
            ResumePdfService service = new ResumePdfServiceImpl(
                    resumeViewService, resumeDocumentService,
                    new ClassicPdfLayout(fileStorageService),
                    new ModernPdfLayout(fileStorageService),
                    new SaraminPdfLayout(fileStorageService));
            given(resumeDocumentService.getDocuments(USER_ID))
                    .willReturn(List.of(new DocumentResponse(DOCUMENT_ID, "대용량", theme, true, null, null, null)));

            byte[] pdf = service.generatePdf(USER_ID, DOCUMENT_ID);
            String text = extract(pdf);

            for (int i = 1; i <= 40; i++) {
                assertThat(text).as("%s: 회사%d 유실", theme, i).contains("회사" + i);
            }
            assertThat(text).contains("한남대학교");
        }
    }

    private ResumeViewResponse multiPageView() {
        List<CareerResponse> careers = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            careers.add(new CareerResponse((long) i, "회사" + i, "백엔드 개발자",
                    LocalDate.of(2020, 1, 1), null,
                    "오래된 경력 설명 문장입니다. ".repeat(30).trim() + " (" + i + ")", i, null, null));
        }
        return new ResumeViewResponse(
                new ProfileResponse(1L, "홍길동", "t@e.com", "010-1234-5678", "대전 서구",
                        LocalDate.of(1996, 1, 1), null, "백엔드 개발자", null, null),
                careers,
                List.of(new EducationResponse(3L, "한남대학교", "대학교", "컴퓨터공학과", "학사", "3.9/4.5",
                        LocalDate.of(2015, 3, 2), LocalDate.of(2020, 2, 20), "졸업", 0, null, null)),
                List.of(new SkillResponse(4L, "Java", "상", "LANGUAGE", 0, null)),
                List.of(), List.of(), List.of(), List.of(), null);
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