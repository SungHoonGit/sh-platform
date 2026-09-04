package com.shplatform.resume.domain;

import static com.shplatform.resume.domain.PdfLayoutSupport.BODY;
import static com.shplatform.resume.domain.PdfLayoutSupport.FAINT;
import static com.shplatform.resume.domain.PdfLayoutSupport.HEAD;
import static com.shplatform.resume.domain.PdfLayoutSupport.MUTED;
import static com.shplatform.resume.domain.PdfLayoutSupport.ON_DARK_MUTED;
import static com.shplatform.resume.domain.PdfLayoutSupport.SLATE_BG;
import static com.shplatform.resume.domain.PdfLayoutSupport.SLATE_SUB;
import static com.shplatform.resume.domain.PdfLayoutSupport.TEAL;
import static com.shplatform.resume.domain.PdfLayoutSupport.TEAL_DARK;
import static com.shplatform.resume.domain.PdfLayoutSupport.WHITE;
import static com.shplatform.resume.domain.PdfLayoutSupport.bold;
import static com.shplatform.resume.domain.PdfLayoutSupport.joinNonBlank;
import static com.shplatform.resume.domain.PdfLayoutSupport.normalizeNewlines;
import static com.shplatform.resume.domain.PdfLayoutSupport.nullIfBlank;
import static com.shplatform.resume.domain.PdfLayoutSupport.periodOrEmpty;
import static com.shplatform.resume.domain.PdfLayoutSupport.regular;
import static com.shplatform.resume.domain.PdfLayoutSupport.hasText;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
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
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 모던(CSS 템플릿) 레이아웃 — 좌측 다크 사이드바와 우측 메인 2단 배치.
 * 사이드바에 사진·이름·연락처와 학력/스킬/자격증, 메인에 그 외 섹션을 배치하고
 * 섹션 제목은 teal 액센트 바(CSS {@code w-1 h-4 bg-teal-600})를 앞에 둔다.
 */
@Component
public class ModernPdfLayout implements ResumePdfLayout {

    /** 사이드바로 강제 배치되는 섹션 (CSS ModernTemplate과 동일). */
    private static final Set<String> SIDE_KEYS = Set.of("educations", "skills", "certificates");

    private final FileStorageService fileStorageService;

    public ModernPdfLayout(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void render(Document document, PdfWriter writer, ResumeViewResponse view,
                       List<String> sectionKeys, Long userId) throws DocumentException {
        PdfPTable page = new PdfPTable(2);
        page.setWidthPercentage(100f);
        page.setWidths(new float[]{30f, 70f});
        page.setKeepTogether(false);

        PdfPCell sidebar = sidebarCell();
        ProfileResponse profile = view.profile();
        renderProfile(sidebar, profile, userId);
        for (String key : sectionKeys) {
            if (SIDE_KEYS.contains(key)) {
                renderSideSection(sidebar, key, view);
            }
        }

        PdfPCell main = mainCell();
        for (String key : sectionKeys) {
            if (!SIDE_KEYS.contains(key)) {
                renderMainSection(main, key, view);
            }
        }

        page.addCell(sidebar);
        page.addCell(main);
        document.add(page);
    }

    private PdfPCell sidebarCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(SLATE_BG);
        cell.setPadding(16f);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        return cell;
    }

    private PdfPCell mainCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingLeft(14f);
        cell.setPaddingRight(2f);
        cell.setPaddingTop(16f);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        return cell;
    }

    private void renderProfile(PdfPCell sidebar, ProfileResponse profile, Long userId) throws DocumentException {
        String name = profile != null && hasText(profile.name()) ? profile.name() : "(이름 미등록)";

        if (profile != null) {
            Image photo = PdfLayoutSupport.loadPhoto(profile, userId, fileStorageService);
            PdfPCell photoCell = PdfLayoutSupport.photoCell(photo);
            if (photoCell != null) {
                PdfPTable photoBox = new PdfPTable(1);
                photoBox.setHorizontalAlignment(Element.ALIGN_CENTER);
                photoBox.setLockedWidth(true);
                photoBox.setTotalWidth(photo.getScaledWidth() + 4f);
                photoBox.setWidths(new float[]{photo.getScaledWidth()});
                photoBox.addCell(photoCell);
                sidebar.addElement(photoBox);
            }
        }

        Paragraph nameP = new Paragraph(name, bold(16f, WHITE));
        nameP.setAlignment(Element.ALIGN_CENTER);
        nameP.setSpacingBefore(profile != null ? 4f : 0f);
        nameP.setSpacingAfter(3f);
        sidebar.addElement(nameP);

        if (profile != null && hasText(profile.headline())) {
            Paragraph headline = new Paragraph(profile.headline(), regular(9.5f, TEAL));
            headline.setAlignment(Element.ALIGN_CENTER);
            headline.setSpacingAfter(6f);
            sidebar.addElement(headline);
        }

        if (profile != null) {
            addSideLabel(sidebar, "연락처");
            addKv(sidebar, "이메일", profile.email());
            addKv(sidebar, "전화번호", profile.phone());
            addKv(sidebar, "주소", profile.address());
            if (profile.birthDate() != null) {
                addKv(sidebar, "생년월일", profile.birthDate().format(PdfLayoutSupport.YMD));
            }
        }
    }

    private void addSideLabel(PdfPCell cell, String label) throws DocumentException {
        Paragraph title = new Paragraph(label, bold(9f, PdfLayoutSupport.FAINT));
        title.setSpacingBefore(6f);
        title.setSpacingAfter(3f);
        cell.addElement(title);
        cell.addElement(new Paragraph(new Chunk(
                new LineSeparator(0.5f, 100f, SLATE_SUB, Element.ALIGN_LEFT, 0))));
    }

    private void addKv(PdfPCell cell, String label, String value) throws DocumentException {
        if (!hasText(value)) {
            return;
        }
        Phrase phrase = new Phrase();
        phrase.add(new Chunk(label + "  ", bold(8f, PdfLayoutSupport.FAINT)));
        phrase.add(new Chunk(value, regular(9f, ON_DARK_MUTED)));
        Paragraph line = new Paragraph(phrase);
        line.setSpacingAfter(3f);
        cell.addElement(line);
    }

    private void renderSideSection(PdfPCell sidebar, String key, ResumeViewResponse view) throws DocumentException {
        switch (key) {
            case "educations" -> {
                if (isNotEmpty(view.educations())) {
                    addSideLabel(sidebar, "학력");
                    for (EducationResponse it : view.educations()) {
                        sideEducation(sidebar, it);
                    }
                }
            }
            case "skills" -> {
                if (isNotEmpty(view.skills())) {
                    addSideLabel(sidebar, "스킬");
                    for (SkillResponse it : view.skills()) {
                        sideSkill(sidebar, it);
                    }
                }
            }
            case "certificates" -> {
                if (isNotEmpty(view.certificates())) {
                    addSideLabel(sidebar, "자격증");
                    for (CertificateResponse it : view.certificates()) {
                        sideCertificate(sidebar, it);
                    }
                }
            }
            default -> { }
        }
    }

    private void sideEducation(PdfPCell cell, EducationResponse item) throws DocumentException {
        Paragraph name = new Paragraph(item.school() == null ? " " : item.school(), bold(10f, WHITE));
        name.setSpacingAfter(1f);
        name.setSpacingBefore(5f);
        cell.addElement(name);

        String sub = joinNonBlank(" · ",
                nullIfBlank(item.major()),
                nullIfBlank(item.degree()),
                hasText(item.gpa()) ? "학점 " + item.gpa() : null,
                nullIfBlank(item.status()));
        if (hasText(sub)) {
            cell.addElement(paragraphSmall(sub, ON_DARK_MUTED, 1f));
        }
        String period = periodOrEmpty(item.startDate(), item.endDate());
        if (hasText(period)) {
            cell.addElement(paragraphSmall(period, PdfLayoutSupport.FAINT, 4f));
        }
    }

    private void sideSkill(PdfPCell cell, SkillResponse item) throws DocumentException {
        String line = "• " + nullIfBlank(item.name())
                + (hasText(item.level()) ? " (" + item.level() + ")" : "");
        cell.addElement(paragraphSmall(line, ON_DARK_MUTED, 4f));
    }

    private void sideCertificate(PdfPCell cell, CertificateResponse item) throws DocumentException {
        String line = nullIfBlank(item.name());
        if (item.acquiredAt() != null) {
            line = joinNonBlank(" · ", line, item.acquiredAt().format(PdfLayoutSupport.YM));
        }
        cell.addElement(paragraphSmall(line, ON_DARK_MUTED, 0.5f));
        if (hasText(item.issuer())) {
            cell.addElement(paragraphSmall(item.issuer(), PdfLayoutSupport.FAINT, 4f));
        }
    }

    private Paragraph paragraphSmall(String text, java.awt.Color color, float spacingAfter) {
        Paragraph p = new Paragraph(text, regular(9f, color));
        p.setSpacingAfter(spacingAfter);
        return p;
    }

    private void renderMainSection(PdfPCell main, String key, ResumeViewResponse view) throws DocumentException {
        switch (key) {
            case "careers" -> {
                if (isNotEmpty(view.careers())) {
                    addMainTitle(main, "경력");
                    for (CareerResponse it : view.careers()) {
                        mainCareer(main, it);
                    }
                }
            }
            case "projects" -> {
                if (isNotEmpty(view.projects())) {
                    addMainTitle(main, "프로젝트");
                    for (ProjectResponse it : view.projects()) {
                        if (!hasProjectContent(it)) {
                            continue;
                        }
                        mainProject(main, it);
                    }
                }
            }
            case "introductions" -> {
                if (isNotEmpty(view.introductions())) {
                    addMainTitle(main, "자기소개");
                    for (IntroductionResponse it : view.introductions()) {
                        mainIntroduction(main, it);
                    }
                }
            }
            case "portfolioItems" -> {
                if (isNotEmpty(view.portfolioItems())) {
                    addMainTitle(main, "포트폴리오");
                    for (PortfolioItemResponse it : view.portfolioItems()) {
                        mainPortfolio(main, it);
                    }
                }
            }
            default -> { }
        }
    }

    private void addMainTitle(PdfPCell cell, String title) throws DocumentException {
        PdfPTable bar = new PdfPTable(2);
        bar.setWidthPercentage(100f);
        bar.setWidths(new float[]{0.6f, 99.4f});
        bar.setSpacingBefore(6f);
        bar.setSpacingAfter(3f);

        PdfPCell accent = new PdfPCell();
        accent.setBackgroundColor(TEAL);
        accent.setFixedHeight(13f);
        accent.setBorder(Rectangle.NO_BORDER);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPaddingLeft(7f);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titleCell.addElement(new Paragraph(title, bold(13f, HEAD)));

        bar.addCell(accent);
        bar.addCell(titleCell);
        cell.addElement(bar);
    }

    private void addMainEntry(PdfPCell cell, String main, String right) throws DocumentException {
        PdfPTable row = new PdfPTable(2);
        row.setWidthPercentage(100f);
        row.setWidths(new float[]{7f, 3f});
        row.setKeepTogether(true);

        PdfPCell left = emptyCell();
        left.addElement(new Paragraph(main, bold(11f, HEAD)));
        PdfPCell rightCell = emptyCell();
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(new Paragraph(right, regular(10f, FAINT)));
        row.addCell(left);
        row.addCell(rightCell);
        cell.addElement(row);
    }

    private void mainCareer(PdfPCell cell, CareerResponse item) throws DocumentException {
        addMainEntry(cell,
                joinNonBlank(" · ", nullIfBlank(item.company()), nullIfBlank(item.title())),
                periodOrEmpty(item.startDate(), item.endDate()));
        if (hasText(item.description())) {
            cell.addElement(paragraphBody(normalizeNewlines(item.description()), 7f));
        }
    }

    private void mainProject(PdfPCell cell, ProjectResponse item) throws DocumentException {
        addMainEntry(cell,
                joinNonBlank(" · ", nullIfBlank(item.name()), nullIfBlank(item.role())),
                periodOrEmpty(item.startDate(), item.endDate()));
        if (hasText(item.techStack())) {
            Paragraph stack = new Paragraph(item.techStack(), regular(9.5f, TEAL_DARK));
            stack.setSpacingAfter(3f);
            cell.addElement(stack);
        }
        if (hasText(item.description())) {
            cell.addElement(paragraphBody(normalizeNewlines(item.description()), 3f));
        }
        if (hasText(item.linkUrl())) {
            Paragraph link = new Paragraph("링크: " + item.linkUrl(), regular(9f, MUTED));
            link.setSpacingAfter(7f);
            cell.addElement(link);
        }
    }

    private void mainIntroduction(PdfPCell cell, IntroductionResponse item) throws DocumentException {
        if (hasText(item.title())) {
            Paragraph title = new Paragraph(item.title(), bold(11f, TEAL_DARK));
            title.setSpacingAfter(2f);
            cell.addElement(title);
        }
        cell.addElement(paragraphBody(normalizeNewlines(item.content() == null ? "" : item.content()), 7f));
    }

    private void mainPortfolio(PdfPCell cell, PortfolioItemResponse item) throws DocumentException {
        String prefix = "FILE".equalsIgnoreCase(item.itemType()) ? "[파일] "
                : "LINK".equalsIgnoreCase(item.itemType()) ? "[링크] " : "";
        Paragraph title = new Paragraph(prefix + (item.title() == null ? "" : item.title()),
                bold(11f, HEAD));
        title.setSpacingAfter(3f);
        cell.addElement(title);
        if ("LINK".equalsIgnoreCase(item.itemType()) && hasText(item.linkUrl())) {
            Paragraph link = new Paragraph("링크: " + item.linkUrl(), regular(9f, MUTED));
            link.setSpacingAfter(2f);
            cell.addElement(link);
        }
        if (hasText(item.description())) {
            cell.addElement(paragraphBody(normalizeNewlines(item.description()), 7f));
        } else if ("FILE".equalsIgnoreCase(item.itemType())
                || "LINK".equalsIgnoreCase(item.itemType())) {
            Paragraph spacer = new Paragraph(" ", regular(6f, MUTED));
            spacer.setSpacingAfter(7f);
            cell.addElement(spacer);
        }
    }

    private Paragraph paragraphBody(String text, float spacingAfter) {
        Paragraph body = new Paragraph(text, regular(10.5f, BODY));
        body.setLeading(12.5f);
        body.setSpacingAfter(spacingAfter);
        body.setKeepTogether(true);
        return body;
    }

    private boolean isNotEmpty(List<?> items) {
        return items != null && !items.isEmpty();
    }

    private static boolean hasProjectContent(ProjectResponse it) {
        return hasText(it.name()) || hasText(it.role()) || hasText(it.description())
                || hasText(it.techStack()) || hasText(it.linkUrl());
    }

    private PdfPCell emptyCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0f);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        return cell;
    }
}