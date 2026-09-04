package com.shplatform.resume.domain;

import static com.shplatform.resume.domain.PdfLayoutSupport.BODY;
import static com.shplatform.resume.domain.PdfLayoutSupport.FAINT;
import static com.shplatform.resume.domain.PdfLayoutSupport.HEAD;
import static com.shplatform.resume.domain.PdfLayoutSupport.INK;
import static com.shplatform.resume.domain.PdfLayoutSupport.MUTED;
import static com.shplatform.resume.domain.PdfLayoutSupport.joinNonBlank;
import static com.shplatform.resume.domain.PdfLayoutSupport.normalizeNewlines;
import static com.shplatform.resume.domain.PdfLayoutSupport.nullIfBlank;
import static com.shplatform.resume.domain.PdfLayoutSupport.periodOrEmpty;
import static com.shplatform.resume.domain.PdfLayoutSupport.regular;
import static com.shplatform.resume.domain.PdfLayoutSupport.ensureRoom;
import static com.shplatform.resume.domain.PdfLayoutSupport.bold;
import static com.shplatform.resume.domain.PdfLayoutSupport.hasText;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
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
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 클래식(CSS 템플릿) 레이아웃 — 단일 컬럼, 전체 텍스트 배열 좌측 정렬.
 * 헤더는 좌측 인적사항 + 우측 프로필 사진(24×32mm)이고,
 * 섹션 제목은 진한 가이드 라인(CSS {@code border-b-2 border-gray-900})으로 구분한다.
 */
@Component
public class ClassicPdfLayout implements ResumePdfLayout {

    private final FileStorageService fileStorageService;

    public ClassicPdfLayout(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void render(Document document, PdfWriter writer, ResumeViewResponse view,
                       List<String> sectionKeys, Long userId) throws DocumentException {
        ProfileResponse profile = view.profile();
        if (profile == null) {
            Paragraph spacer = new Paragraph(" ", regular(1f, INK));
            spacer.setLeading(1f);
            document.add(spacer);
        } else {
            renderHeader(document, profile, userId);
        }
        for (String key : sectionKeys) {
            ensureRoom(document, writer, PdfLayoutSupport.MIN_SECTION_SPACE);
            renderSection(document, key, view);
        }
    }

    private void renderHeader(Document document, ProfileResponse profile, Long userId) throws DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100f);
        header.setWidths(new float[]{7f, 3f});
        header.setKeepTogether(true);

        PdfPCell left = emptyCell();
        left.addElement(new Paragraph(
                hasText(profile.name()) ? profile.name() : "(이름 미등록)", bold(24f, INK)));

        if (hasText(profile.headline())) {
            Paragraph headline = new Paragraph(profile.headline(), regular(11.5f, MUTED));
            headline.setSpacingAfter(5f);
            headline.setSpacingBefore(4f);
            left.addElement(headline);
        }

        String contact = joinNonBlank("  ·  ",
                nullIfBlank(profile.email()),
                nullIfBlank(profile.phone()),
                nullIfBlank(profile.address()),
                profile.birthDate() != null
                        ? profile.birthDate().format(PdfLayoutSupport.YMD) : null);
        if (hasText(contact)) {
            Paragraph contactP = new Paragraph(contact, regular(10f, MUTED));
            contactP.setSpacingAfter(10f);
            left.addElement(contactP);
        }
        header.addCell(left);

        Image photo = PdfLayoutSupport.loadPhoto(profile, userId, fileStorageService);
        PdfPCell photoCell = PdfLayoutSupport.photoCell(photo);
        if (photoCell != null) {
            photoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            header.addCell(photoCell);
        } else {
            header.addCell(emptyCell());
        }

        document.add(header);
        document.add(new Chunk(new LineSeparator(1.1f, 100f, HEAD, Element.ALIGN_LEFT, 4)));
    }

    private void renderSection(Document document, String key, ResumeViewResponse view) throws DocumentException {
        switch (key) {
            case "careers" -> renderGroup(document, "경력", view.careers(), (d, it) -> renderCareer(d, it));
            case "projects" -> renderGroup(document, "프로젝트", view.projects(), (d, it) -> renderProject(d, it));
            case "educations" -> renderGroup(document, "학력", view.educations(), (d, it) -> renderEducation(d, it));
            case "skills" -> renderGroup(document, "스킬", view.skills(), (d, it) -> renderSkill(d, it));
            case "certificates" ->
                    renderGroup(document, "자격증", view.certificates(), (d, it) -> renderCertificate(d, it));
            case "introductions" ->
                    renderGroup(document, "자기소개", view.introductions(), (d, it) -> renderIntroduction(d, it));
            case "portfolioItems" ->
                    renderGroup(document, "포트폴리오", view.portfolioItems(), (d, it) -> renderPortfolio(d, it));
            default -> { }
        }
    }

    private <T> void renderGroup(Document document, String title, List<T> items,
                                 SectionRenderer<T> renderer) throws DocumentException {
        if (items == null || items.isEmpty()) {
            return;
        }
        addSectionTitle(document, title);
        for (T item : items) {
            renderer.render(document, item);
        }
    }

    @FunctionalInterface
    private interface SectionRenderer<T> {
        void render(Document document, T item) throws DocumentException;
    }

    private void addSectionTitle(Document document, String title) throws DocumentException {
        Paragraph section = new Paragraph(title, bold(13f, HEAD));
        section.setSpacingBefore(8f);
        section.setSpacingAfter(3f);
        section.setKeepTogether(true);
        document.add(section);
        document.add(new Chunk(new LineSeparator(0.9f, 100f, HEAD, Element.ALIGN_LEFT, 0)));
    }

    private void renderCareer(Document document, CareerResponse item) throws DocumentException {
        addEntryLine(document,
                joinNonBlank(" · ", nullIfBlank(item.company()), nullIfBlank(item.title())),
                periodOrEmpty(item.startDate(), item.endDate()));
        if (hasText(item.description())) {
            document.add(bodyParagraph(normalizeNewlines(item.description()), 6f));
        }
    }

    private void renderEducation(Document document, EducationResponse item) throws DocumentException {
        addEntryLine(document, item.school() == null ? "" : item.school(),
                periodOrEmpty(item.startDate(), item.endDate()));
        String sub = joinNonBlank(" · ",
                nullIfBlank(item.schoolType()),
                nullIfBlank(item.major()),
                nullIfBlank(item.degree()),
                hasText(item.gpa()) ? "학점 " + item.gpa() : null,
                nullIfBlank(item.status()));
        if (hasText(sub)) {
            Paragraph subP = new Paragraph(sub, regular(10f, MUTED));
            subP.setSpacingAfter(8f);
            document.add(subP);
        }
    }

    private void renderSkill(Document document, SkillResponse item) throws DocumentException {
        String line = "• " + nullIfBlank(item.name())
                + (hasText(item.level()) ? " (" + item.level() + ")" : "")
                + (hasText(item.category()) ? " · " + item.category() : "");
        Paragraph skill = new Paragraph(line, regular(10.5f, BODY));
        skill.setSpacingAfter(4f);
        document.add(skill);
    }

    private void renderCertificate(Document document, CertificateResponse item) throws DocumentException {
        addEntryLine(document, item.name() == null ? "" : item.name(),
                item.acquiredAt() != null ? item.acquiredAt().format(PdfLayoutSupport.YM) : "");
        if (hasText(item.issuer())) {
            Paragraph issuer = new Paragraph(item.issuer(), regular(10f, MUTED));
            issuer.setSpacingAfter(8f);
            document.add(issuer);
        }
    }

    private void renderProject(Document document, ProjectResponse item) throws DocumentException {
        addEntryLine(document,
                joinNonBlank(" · ", nullIfBlank(item.name()), nullIfBlank(item.role())),
                periodOrEmpty(item.startDate(), item.endDate()));
        if (hasText(item.techStack())) {
            Paragraph stack = new Paragraph(item.techStack(), regular(10f, MUTED));
            stack.setSpacingAfter(3f);
            document.add(stack);
        }
        if (hasText(item.description())) {
            document.add(bodyParagraph(normalizeNewlines(item.description()), 3f));
        }
        if (hasText(item.linkUrl())) {
            Paragraph link = new Paragraph("링크: " + item.linkUrl(), regular(9.5f, FAINT));
            link.setSpacingAfter(8f);
            document.add(link);
        }
    }

    private void renderIntroduction(Document document, IntroductionResponse item) throws DocumentException {
        if (hasText(item.title())) {
            Paragraph title = new Paragraph(item.title(), bold(11.5f, HEAD));
            title.setSpacingAfter(2f);
            document.add(title);
        }
        document.add(bodyParagraph(normalizeNewlines(item.content() == null ? "" : item.content()), 7f));
    }

    private void renderPortfolio(Document document, PortfolioItemResponse item) throws DocumentException {
        String prefix = "FILE".equalsIgnoreCase(item.itemType()) ? "[파일] "
                : "LINK".equalsIgnoreCase(item.itemType()) ? "[링크] " : "";
        Paragraph title = new Paragraph(prefix + (item.title() == null ? "" : item.title()),
                bold(11.5f, HEAD));
        title.setSpacingAfter(3f);
        document.add(title);
        if ("LINK".equalsIgnoreCase(item.itemType()) && hasText(item.linkUrl())) {
            Paragraph link = new Paragraph("링크: " + item.linkUrl(), regular(9.5f, FAINT));
            link.setSpacingAfter(2f);
            document.add(link);
        }
        if (hasText(item.description())) {
            document.add(bodyParagraph(normalizeNewlines(item.description()), 7f));
        } else if ("FILE".equalsIgnoreCase(item.itemType())
                || "LINK".equalsIgnoreCase(item.itemType())) {
            Paragraph spacer = new Paragraph(" ", regular(6f, MUTED));
            spacer.setSpacingAfter(7f);
            document.add(spacer);
        }
    }

    /** 본문 문단. 개행 보존, 문단 간 여백(스페이싱) 적용. */
    private Paragraph bodyParagraph(String text, float spacingAfter) {
        Paragraph body = new Paragraph(text, regular(10.5f, BODY));
        body.setLeading(12.5f);
        body.setSpacingAfter(spacingAfter);
        body.setKeepTogether(true);
        return body;
    }

    /** 항목 제목(좌) + 기간(우) 2열 행. 클래식은 항목 구분선을 두지 않고 여백으로 나눈다. */
    private void addEntryLine(Document document, String main, String right) throws DocumentException {
        PdfPTable row = new PdfPTable(2);
        row.setWidthPercentage(100f);
        row.setWidths(new float[]{7f, 3f});
        row.setKeepTogether(true);
        row.setSpacingBefore(1f);

        PdfPCell left = emptyCell();
        left.addElement(new Paragraph(main.isEmpty() ? " " : main, bold(11f, HEAD)));
        PdfPCell rightCell = emptyCell();
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(new Paragraph(right, regular(10f, MUTED)));
        row.addCell(left);
        row.addCell(rightCell);
        document.add(row);
    }

    private PdfPCell emptyCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0f);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        return cell;
    }
}