package com.shplatform.resume.domain;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.shplatform.resume.api.dto.CareerResponse;
import com.shplatform.resume.api.dto.CertificateResponse;
import com.shplatform.resume.api.dto.EducationResponse;
import com.shplatform.resume.api.dto.IntroductionResponse;
import com.shplatform.resume.api.dto.PortfolioItemResponse;
import com.shplatform.resume.api.dto.ProfileResponse;
import com.shplatform.resume.api.dto.ProjectResponse;
import com.shplatform.resume.api.dto.ResumeViewResponse;
import com.shplatform.resume.api.dto.SkillResponse;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * OpenPDF 기반 이력서 PDF 생성 구현.
 *
 * <p>A4 크기 단일 서식으로 렌더링하며, 한글은 classpath {@code fonts/}의
 * Spoqa Han Sans (SIL OFL 1.1) TrueType을 임베드한다. 외부 프로세스/인프라 의존 없음.
 */
@Service
public class ResumePdfServiceImpl implements ResumePdfService {

    private static final float MARGIN_MM_18 = 51f;
    private static final float MARGIN_MM_16 = 45f;

    private static final Color TEXT = new Color(0x1F2937);
    private static final Color BODY = new Color(0x374151);
    private static final Color MUTED = new Color(0x6B7280);
    private static final Color ACCENT = new Color(0x0D9488);
    private static final Color RULE = new Color(0xE5E7EB);

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy.MM");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final ResumeViewService resumeViewService;

    public ResumePdfServiceImpl(ResumeViewService resumeViewService) {
        this.resumeViewService = resumeViewService;
    }

    /** classpath 리소스에서 한글 폰트를 1회 로드 (JVM 클래스 초기화 시점, 스레드 안전). */
    private static final class Fonts {
        static final BaseFont REGULAR = load("/fonts/SpoqaHanSansRegular.ttf");
        static final BaseFont BOLD = load("/fonts/SpoqaHanSansBold.ttf");

        private static BaseFont load(String path) {
            try (InputStream in = ResumePdfServiceImpl.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new IllegalStateException("폰트 리소스 없음: " + path);
                }
                byte[] bytes = in.readAllBytes();
                return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false, bytes, null);
            } catch (IOException | DocumentException e) {
                throw new IllegalStateException("폰트 로드 실패: " + path, e);
            }
        }
    }

    @Override
    public byte[] generatePdf(Long userId) {
        ResumeViewResponse view = resumeViewService.getMyResumeView(userId);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             Document document = new Document(PageSize.A4, MARGIN_MM_18, MARGIN_MM_18, MARGIN_MM_16, MARGIN_MM_16)) {
            PdfWriter.getInstance(document, bos);
            document.open();

            renderHeader(document, view.profile());

            if (!orEmpty(view.introductions()).isEmpty()) {
                addSectionTitle(document, "자기소개");
                for (IntroductionResponse item : orEmpty(view.introductions())) {
                    renderIntroduction(document, item);
                }
            }
            if (!orEmpty(view.careers()).isEmpty()) {
                addSectionTitle(document, "경력");
                for (CareerResponse item : orEmpty(view.careers())) {
                    renderCareer(document, item);
                }
            }
            if (!orEmpty(view.educations()).isEmpty()) {
                addSectionTitle(document, "학력");
                for (EducationResponse item : orEmpty(view.educations())) {
                    renderEducation(document, item);
                }
            }
            if (!orEmpty(view.skills()).isEmpty()) {
                addSectionTitle(document, "스킬");
                for (SkillResponse item : orEmpty(view.skills())) {
                    renderSkill(document, item);
                }
            }
            if (!orEmpty(view.certificates()).isEmpty()) {
                addSectionTitle(document, "자격증");
                for (CertificateResponse item : orEmpty(view.certificates())) {
                    renderCertificate(document, item);
                }
            }
            if (!orEmpty(view.projects()).isEmpty()) {
                addSectionTitle(document, "프로젝트");
                for (ProjectResponse item : orEmpty(view.projects())) {
                    renderProject(document, item);
                }
            }
            if (!orEmpty(view.portfolioItems()).isEmpty()) {
                addSectionTitle(document, "포트폴리오");
                for (PortfolioItemResponse item : orEmpty(view.portfolioItems())) {
                    renderPortfolio(document, item);
                }
            }

            document.close();
            return bos.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("이력서 PDF 생성 실패: userId=" + userId, e);
        }
    }

    private void renderHeader(Document document, ProfileResponse profile) throws DocumentException {
        if (profile == null) {
            Paragraph spacer = new Paragraph(" ", font(Fonts.REGULAR, 1f, TEXT));
            spacer.setLeading(1f);
            document.add(spacer);
            return;
        }
        String name = hasText(profile.name()) ? profile.name() : "(이름 미등록)";
        Paragraph nameP = new Paragraph(name, font(Fonts.BOLD, 26f, TEXT));
        nameP.setSpacingAfter(4f);
        document.add(nameP);

        if (hasText(profile.headline())) {
            Paragraph headline = new Paragraph(profile.headline(), font(Fonts.REGULAR, 11.5f, MUTED));
            headline.setSpacingAfter(4f);
            document.add(headline);
        }

        String contact = joinNonBlank("  ·  ",
                nullIfBlank(profile.email()),
                nullIfBlank(profile.phone()),
                nullIfBlank(profile.address()),
                profile.birthDate() != null ? profile.birthDate().format(YMD) : null);
        if (hasText(contact)) {
            Paragraph contactP = new Paragraph(contact, font(Fonts.REGULAR, 10f, MUTED));
            contactP.setSpacingAfter(8f);
            document.add(contactP);
        }
        document.add(new Chunk(new LineSeparator(0.8f, 100f, ACCENT, Element.ALIGN_LEFT, 0)));
    }

    private void addSectionTitle(Document document, String title) throws DocumentException {
        Paragraph section = new Paragraph(title, font(Fonts.BOLD, 14f, TEXT));
        section.setSpacingBefore(16f);
        section.setSpacingAfter(6f);
        section.setKeepTogether(true);
        document.add(section);
        document.add(new Chunk(new LineSeparator(0.6f, 100f, RULE, Element.ALIGN_LEFT, -2)));
    }

    private void renderIntroduction(Document document, IntroductionResponse item) throws DocumentException {
        Paragraph title;
        if (hasText(item.title())) {
            title = new Paragraph(item.title(), font(Fonts.BOLD, 11.5f, TEXT));
            title.setSpacingAfter(2f);
            document.add(title);
        }
        Paragraph body = new Paragraph(normalizeNewlines(item.content() == null ? "" : item.content()),
                font(Fonts.REGULAR, 10.5f, BODY));
        body.setLeading(15f);
        body.setSpacingAfter(4f);
        body.setKeepTogether(true);
        document.add(body);
    }

    private void renderCareer(Document document, CareerResponse item) throws DocumentException {
        String main = joinNonBlank(" · ", nullIfBlank(item.company()), nullIfBlank(item.title()));
        addEntryLine(document, main, periodOrEmpty(item.startDate(), item.endDate()));
        if (hasText(item.description())) {
            Paragraph desc = new Paragraph(normalizeNewlines(item.description()), font(Fonts.REGULAR, 10.5f, BODY));
            desc.setLeading(15f);
            desc.setSpacingAfter(6f);
            desc.setKeepTogether(true);
            document.add(desc);
        }
    }

    private void renderEducation(Document document, EducationResponse item) throws DocumentException {
        addEntryLine(document, nullIfBlank(item.school()) == null ? "" : item.school(),
                periodOrEmpty(item.startDate(), item.endDate()));
        String sub = joinNonBlank(" · ",
                nullIfBlank(item.schoolType()),
                nullIfBlank(item.major()),
                nullIfBlank(item.degree()),
                hasText(item.gpa()) ? "학점 " + item.gpa() : null,
                nullIfBlank(item.status()));
        if (hasText(sub)) {
            Paragraph subP = new Paragraph(sub, font(Fonts.REGULAR, 10f, MUTED));
            subP.setSpacingAfter(6f);
            document.add(subP);
        }
    }

    private void renderSkill(Document document, SkillResponse item) throws DocumentException {
        String line = "• " + nullIfBlank(item.name())
                + (hasText(item.level()) ? " (" + item.level() + ")" : "")
                + (hasText(item.category()) ? " · " + item.category() : "");
        Paragraph skill = new Paragraph(line, font(Fonts.REGULAR, 10.5f, BODY));
        skill.setSpacingAfter(3f);
        document.add(skill);
    }

    private void renderCertificate(Document document, CertificateResponse item) throws DocumentException {
        String right = item.acquiredAt() != null ? item.acquiredAt().format(YM) : "";
        addEntryLine(document, nullIfBlank(item.name()) == null ? "" : item.name(), right);
        if (hasText(item.issuer())) {
            Paragraph issuer = new Paragraph(item.issuer(), font(Fonts.REGULAR, 10f, MUTED));
            issuer.setSpacingAfter(6f);
            document.add(issuer);
        }
    }

    private void renderProject(Document document, ProjectResponse item) throws DocumentException {
        String main = joinNonBlank(" · ", nullIfBlank(item.name()), nullIfBlank(item.role()));
        addEntryLine(document, main, periodOrEmpty(item.startDate(), item.endDate()));
        if (hasText(item.techStack())) {
            Paragraph stack = new Paragraph(item.techStack(), font(Fonts.REGULAR, 10f, MUTED));
            stack.setSpacingAfter(2f);
            document.add(stack);
        }
        if (hasText(item.description())) {
            Paragraph desc = new Paragraph(normalizeNewlines(item.description()), font(Fonts.REGULAR, 10.5f, BODY));
            desc.setLeading(15f);
            desc.setSpacingAfter(2f);
            desc.setKeepTogether(true);
            document.add(desc);
        }
        if (hasText(item.linkUrl())) {
            Paragraph link = new Paragraph("링크: " + item.linkUrl(), font(Fonts.REGULAR, 9.5f, MUTED));
            link.setSpacingAfter(7f);
            document.add(link);
        }
    }

    private void renderPortfolio(Document document, PortfolioItemResponse item) throws DocumentException {
        Paragraph title = new Paragraph(nullIfBlank(item.title()) == null ? "" : item.title(),
                font(Fonts.BOLD, 11.5f, TEXT));
        title.setSpacingAfter(2f);
        document.add(title);
        if ("LINK".equalsIgnoreCase(item.itemType()) && hasText(item.linkUrl())) {
            Paragraph link = new Paragraph("링크: " + item.linkUrl(), font(Fonts.REGULAR, 10f, MUTED));
            link.setSpacingAfter(2f);
            document.add(link);
        }
        if (hasText(item.description())) {
            Paragraph desc = new Paragraph(item.description(), font(Fonts.REGULAR, 10.5f, BODY));
            desc.setLeading(15f);
            desc.setSpacingAfter(7f);
            document.add(desc);
        } else if ("FILE".equalsIgnoreCase(item.itemType())) {
            Paragraph empty = new Paragraph(" ", font(Fonts.REGULAR, 6f, MUTED));
            empty.setSpacingAfter(7f);
            document.add(empty);
        }
    }

    /** 제목(좌측)과 기간(우측 정렬) 2열 행을 추가한다. */
    private void addEntryLine(Document document, String main, String right) throws DocumentException {
        PdfPTable row = new PdfPTable(2);
        row.setWidthPercentage(100f);
        row.setWidths(new float[]{7f, 3f});
        row.setKeepTogether(true);

        PdfPCell left = cell(new Paragraph(main, font(Fonts.BOLD, 11.5f, TEXT)));
        PdfPCell rightCell = cell(new Paragraph(right, font(Fonts.REGULAR, 10f, MUTED)));
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setPaddingTop(2f);
        left.setPaddingTop(2f);

        row.addCell(left);
        row.addCell(rightCell);
        document.add(row);
        document.add(new Chunk(new LineSeparator(0.3f, 100f, RULE, Element.ALIGN_LEFT, -1)));
    }

    private PdfPCell cell(Paragraph paragraph) {
        PdfPCell cell = new PdfPCell(paragraph);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0f);
        return cell;
    }

    private Font font(BaseFont baseFont, float size, Color color) {
        return new Font(baseFont, size, Font.NORMAL, color);
    }

    private String periodOrEmpty(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return "";
        }
        return period(start, end);
    }

    private String period(LocalDate start, LocalDate end) {
        String from = start != null ? start.format(YM) : "";
        String to = end != null ? end.format(YM) : "현재";
        return (from.isEmpty() ? "" : from + " – ") + to;
    }

    private String normalizeNewlines(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nullIfBlank(String value) {
        return hasText(value) ? value : null;
    }

    private String joinNonBlank(String separator, String... parts) {
        List<String> kept = new ArrayList<>();
        for (String part : parts) {
            if (hasText(part)) {
                kept.add(part);
            }
        }
        return String.join(separator, kept);
    }

    private <T> List<T> orEmpty(List<T> list) {
        return list != null ? list : List.of();
    }
}