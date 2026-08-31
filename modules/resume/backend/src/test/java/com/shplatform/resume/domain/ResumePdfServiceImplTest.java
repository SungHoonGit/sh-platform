package com.shplatform.resume.domain;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.shplatform.resume.api.dto.CareerResponse;
import com.shplatform.resume.api.dto.CertificateResponse;
import com.shplatform.resume.api.dto.DocumentResponse;
import com.shplatform.resume.api.dto.EducationResponse;
import com.shplatform.resume.api.dto.IntroductionResponse;
import com.shplatform.resume.api.dto.PortfolioItemResponse;
import com.shplatform.resume.api.dto.ProfileResponse;
import com.shplatform.resume.api.dto.ProjectResponse;
import com.shplatform.resume.api.dto.ResumeViewResponse;
import com.shplatform.resume.api.dto.SkillResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ResumePdfServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long DOCUMENT_ID = 100L;

    /** 1x1 투명 PNG — 프로필 사진 렌더 검증용. */
    private static final byte[] ONE_PX_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    @Mock
    private ResumeViewService resumeViewService;
    @Mock
    private ResumeDocumentService resumeDocumentService;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ResumePdfServiceImpl resumePdfService;

    @Test
    @DisplayName("generatePdf: 전체 항목이 있으면 유효한 PDF를 생성하고 내용이 포함된다")
    void generatePdf_buildsValidPdfWithContent() throws Exception {
        given(resumeViewService.getMyResumeView(USER_ID)).willReturn(fullView(profile(null)));

        byte[] pdf = resumePdfService.generatePdf(USER_ID, null);

        assertThat(new String(pdf, 0, 8, StandardCharsets.US_ASCII)).startsWith("%PDF");
        assertThat(pdf).hasSizeGreaterThan(1_000);
        String text = extractText(pdf);
        assertThat(text).contains("홍길동");
        assertThat(text).contains("경력");
        assertThat(text).contains("네이버");
        assertThat(text).contains("Spring Boot");
    }

    @Test
    @DisplayName("generatePdf: 프로필 미등록·항목 전무여도 유효한 PDF를 생성한다")
    void generatePdf_emptyResumeStillGenerates() throws Exception {
        given(resumeViewService.getMyResumeView(USER_ID))
                .willReturn(new ResumeViewResponse(null, List.of(), List.of(), List.of(),
                        List.of(), List.of(), List.of(), List.of(), null));

        byte[] pdf = resumePdfService.generatePdf(USER_ID, null);

        assertThat(new String(pdf, 0, 8, StandardCharsets.US_ASCII)).startsWith("%PDF");
        assertThat(pdf).hasSizeGreaterThan(500);
        assertThat(extractText(pdf)).doesNotContain("경력");
    }

    @Test
    @DisplayName("generatePdf: 날짜가 null인 재직중 경력도 오류 없이 렌더링된다")
    void generatePdf_nullDatesRenderedSafely() throws Exception {
        given(resumeViewService.getMyResumeView(USER_ID)).willReturn(fullView(profile(null)));

        byte[] pdf = resumePdfService.generatePdf(USER_ID, null);

        assertThat(extractText(pdf)).contains("현재");
    }

    @Test
    @DisplayName("generatePdf: 문서 sectionConfig가 지정되면 포함 섹션만 그 순서대로 렌더링한다")
    void generatePdf_respectsDocumentSectionConfig() throws Exception {
        given(resumeViewService.getMyResumeView(USER_ID)).willReturn(fullView(profile(null)));
        String config = """
                [
                  {"key":"careers","included":true,"order":1},
                  {"key":"projects","included":true,"order":2},
                  {"key":"educations","included":false,"order":3},
                  {"key":"skills","included":false,"order":4},
                  {"key":"certificates","included":false,"order":5},
                  {"key":"introductions","included":false,"order":6},
                  {"key":"portfolioItems","included":true,"order":7}
                ]""";
        given(resumeDocumentService.getDocuments(USER_ID))
                .willReturn(List.of(new DocumentResponse(DOCUMENT_ID, "제출용", "CLASSIC", true, config, null, null)));

        byte[] pdf = resumePdfService.generatePdf(USER_ID, DOCUMENT_ID);

        String text = extractText(pdf);
        assertThat(text).contains("경력");
        assertThat(text).contains("프로젝트");
        assertThat(text).contains("포트폴리오");
        assertThat(text).doesNotContain("자격증");
        assertThat(text).doesNotContain("스킬");
    }

    @Test
    @DisplayName("generatePdf: 프로필 사진 파일을 읽어 임베드하고 유효한 PDF를 생성한다")
    void generatePdf_includesProfilePhoto() throws Exception {
        ProfileResponse withPhoto = profile("/api/v1/files/42/download");
        given(resumeViewService.getMyResumeView(USER_ID)).willReturn(fullView(withPhoto));
        given(fileStorageService.download(USER_ID, 42L))
                .willReturn(new FileStorageService.DownloadedFile("photo.png", "image/png", ONE_PX_PNG));

        byte[] pdf = resumePdfService.generatePdf(USER_ID, null);

        assertThat(new String(pdf, 0, 8, StandardCharsets.US_ASCII)).startsWith("%PDF");
        verify(fileStorageService).download(eq(USER_ID), eq(42L));
    }

    @Test
    @DisplayName("generatePdf: 사진 파일이 없어도(NOT_FOUND) PDF 생성이 실패하지 않는다")
    void generatePdf_photoMissingStillGenerates() throws Exception {
        ProfileResponse withPhoto = profile("/api/v1/files/999/download");
        given(resumeViewService.getMyResumeView(USER_ID)).willReturn(fullView(withPhoto));
        given(fileStorageService.download(USER_ID, 999L))
                .willThrow(new com.shplatform.common.exception.BusinessException(
                        com.shplatform.common.exception.ErrorCode.NOT_FOUND));

        byte[] pdf = resumePdfService.generatePdf(USER_ID, null);

        assertThat(new String(pdf, 0, 8, StandardCharsets.US_ASCII)).startsWith("%PDF");
    }

    private ResumeViewResponse fullView(ProfileResponse profile) {
        CareerResponse career = new CareerResponse(
                1L, "네이버", "백엔드 개발자", LocalDate.of(2022, 3, 1), null,
                "검색 서비스 API 개발", 0, null, null);
        CareerResponse oldCareer = new CareerResponse(
                2L, "카카오", "주니어 개발자", LocalDate.of(2020, 1, 1), LocalDate.of(2022, 2, 28),
                "앱 백엔드 유지보수", 1, null, null);
        EducationResponse education = new EducationResponse(
                3L, "한남대학교", "대학교", "컴퓨터공학과", "학사", "3.9 / 4.5",
                LocalDate.of(2015, 3, 2), LocalDate.of(2020, 2, 20), "졸업", 0, null, null);
        SkillResponse skill = new SkillResponse(4L, "Java", "상", "LANGUAGE", 0, null);
        SkillResponse skill2 = new SkillResponse(5L, "Spring Boot", "상", "FRAMEWORK", 1, null);
        CertificateResponse cert = new CertificateResponse(
                6L, "정보처리기사", "한국산업인력공단", LocalDate.of(2021, 6, 1), 0, null);
        ProjectResponse project = new ProjectResponse(
                7L, "sh-platform", "백엔드·풀스택", LocalDate.of(2026, 1, 1), null,
                "채용공고 스크래핑 플랫폼", "Java · Spring Boot · React", "https://github.com/example", 0, null, null);
        IntroductionResponse intro = new IntroductionResponse(
                8L, "지원동기", "데이터를 좋아하는 개발자입니다.", 0, null, null);
        PortfolioItemResponse portfolio = new PortfolioItemResponse(
                9L, "포트폴리오", "LINK", null, "https://example.com/portfolio", "작업물 모음", 0, null);

        return new ResumeViewResponse(profile, List.of(career, oldCareer), List.of(education),
                List.of(skill, skill2), List.of(cert), List.of(project), List.of(intro),
                List.of(portfolio), null);
    }

    private ProfileResponse profile(String photoUrl) {
        return new ProfileResponse(
                10L, "홍길동", "test@example.com", "010-1234-5678", "대전 서구",
                LocalDate.of(1996, 1, 15), photoUrl, "백엔드 개발자", null, null);
    }

    private String extractText(byte[] pdf) throws Exception {
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