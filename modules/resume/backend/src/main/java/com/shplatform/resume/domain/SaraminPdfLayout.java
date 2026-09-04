package com.shplatform.resume.domain;

import static com.shplatform.resume.domain.PdfLayoutSupport.BODY;
import static com.shplatform.resume.domain.PdfLayoutSupport.BORDER;
import static com.shplatform.resume.domain.PdfLayoutSupport.FAINT;
import static com.shplatform.resume.domain.PdfLayoutSupport.HEAD;
import static com.shplatform.resume.domain.PdfLayoutSupport.MUTED;
import static com.shplatform.resume.domain.PdfLayoutSupport.RULE_LIGHT;
import static com.shplatform.resume.domain.PdfLayoutSupport.SLATE_50;
import static com.shplatform.resume.domain.PdfLayoutSupport.bold;
import static com.shplatform.resume.domain.PdfLayoutSupport.ensureRoom;
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
 * 사람인형(CSS 템플릿) 레이아웃 — 박스형 섹션 + 라벨 연락처 표.
 * 상단 프로필 박스(테두리 2px)에 사진·이름·한 줄 소개와 라벨-값 연락처 그리드를 두고,
 * 섹션마다 회색 테두리 박스 + {@code bg-slate-100} 제목 바를 그린다.
 * 경력은 회사명/직무/기간 3열 표, 학력은 기간+세부 2열로 렌더링한다.
 */
@Component
public class SaraminPdfLayout implements ResumePdfLayout {

    private final FileStorageService fileStorageService;

    public SaraminPdfLayout(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void render(Document document, PdfWriter writer, ResumeViewResponse view,
                       List<String> sectionKeys, Long userId) throws DocumentException {
        renderProfileBox(document, view.profile(), userId);

        for (String key : sectionKeys) {
            ensureRoom(document, writer, PdfLayoutSupport.MIN_SECTION_SPACE);
            renderSection(document, key, view);
        }
    }

    private void renderProfileBox(Document document, ProfileResponse profile, Long userId) throws DocumentException {
        PdfPTable outer = new PdfPTable(2);
        outer.setWidthPercentage(100f);
        outer.setWidths(new float[]{28f, 72f});
        outer.setKeepTogether(true);

        Image photo = profile != null
                ? PdfLayoutSupport.loadPhoto(profile, userId, fileStorageService) : null;
        PdfPCell photoSide = new PdfPCell();
        photoSide.setBorder(Rectangle.TOP | Rectangle.BOTTOM | Rectangle.LEFT);
        photoSide.setBorderColor(HEAD);
        photoSide.setBorderWidth(1.2f);
        photoSide.setPadding(8f);
        photoSide.setVerticalAlignment(Element.ALIGN_MIDDLE);
        PdfPCell photoCell = PdfLayoutSupport.photoCell(photo);
        if (photoCell != null) {
            PdfPTable photoBox = new PdfPTable(1);
            photoBox.setHorizontalAlignment(Element.ALIGN_CENTER);
            photoBox.setLockedWidth(true);
            photoBox.setTotalWidth(photo.getScaledWidth() + 4f);
            photoBox.setWidths(new float[]{photo.getScaledWidth()});
            photoBox.addCell(photoCell);
            photoSide.addElement(photoBox);
        }
        outer.addCell(photoSide);

        PdfPCell contentSide = new PdfPCell();
        contentSide.setBorder(Rectangle.TOP | Rectangle.BOTTOM | Rectangle.RIGHT);
        contentSide.setBorderColor(HEAD);
        contentSide.setBorderWidth(1.2f);
        contentSide.setPadding(12f);
        contentSide.setVerticalAlignment(Element.ALIGN_TOP);

        String name = profile != null && hasText(profile.name()) ? profile.name() : "(이름 미등록)";
        Paragraph nameP = new Paragraph(name, bold(20f, HEAD));
        nameP.setSpacingAfter(4f);
        contentSide.addElement(nameP);

        if (profile != null && hasText(profile.headline())) {
            Paragraph headline = new Paragraph(profile.headline(), regular(10.5f, MUTED));
            headline.setSpacingAfter(8f);
            contentSide.addElement(headline);
        }

        addContactGrid(contentSide, profile);
        outer.addCell(contentSide);

        document.add(outer);
        document.add(spacer(10f));
    }

    private void addContactGrid(PdfPCell cell, ProfileResponse profile) throws DocumentException {
        PdfPTable grid = new PdfPTable(2);
        grid.setWidthPercentage(100f);
        grid.setWidths(new float[]{22f, 78f});

        addLabelValueRow(grid, "이메일", profile != null ? profile.email() : null);
        addLabelValueRow(grid, "전화번호", profile != null ? profile.phone() : null);
        addLabelValueRow(grid, "생년월일", profile != null && profile.birthDate() != null
                ? profile.birthDate().format(PdfLayoutSupport.YMD) : null);
        addLabelValueRow(grid, "주소", profile != null ? profile.address() : null);

        cell.addElement(grid);
    }

    private void addLabelValueRow(PdfPTable grid, String label, String value) throws DocumentException {
        PdfPCell labelCell = new PdfPCell(new Paragraph(label, bold(8.5f, MUTED)));
        labelCell.setBackgroundColor(SLATE_50);
        labelCell.setBorder(Rectangle.BOX);
        labelCell.setBorderColor(RULE_LIGHT);
        labelCell.setBorderWidth(0.4f);
        labelCell.setPadding(4f);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPCell valueCell = new PdfPCell(
                new Paragraph(hasText(value) ? value : "-", regular(9f, BODY)));
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setBorderColor(RULE_LIGHT);
        valueCell.setBorderWidth(0.4f);
        valueCell.setPadding(4f);

        grid.addCell(labelCell);
        grid.addCell(valueCell);
    }

    private void renderSection(Document document, String key, ResumeViewResponse view) throws DocumentException {
        switch (key) {
            case "careers" -> box(document, "경력", cell -> careerBody(cell, view.careers()));
            case "projects" -> box(document, "프로젝트", cell -> projectBody(cell, view.projects()));
            case "educations" -> box(document, "학력", cell -> educationBody(cell, view.educations()));
            case "skills" -> box(document, "스킬", cell -> skillBody(cell, view.skills()));
            case "certificates" -> box(document, "자격증", cell -> certificateBody(cell, view.certificates()));
            case "introductions" -> box(document, "자기소개",
                    cell -> introductionBody(cell, view.introductions()));
            case "portfolioItems" -> box(document, "포트폴리오",
                    cell -> portfolioBody(cell, view.portfolioItems()));
            default -> { }
        }
    }

    /** 제목 바(bg-slate-100) + 본문을 회색 테두리 박스로 감싸 추가한다. */
    private void box(Document document, String title, BoxContent body) throws DocumentException {
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100f);
box.setSpacingBefore(6f);
box.setSpacingAfter(2f);
        box.setKeepTogether(true);

        PdfPCell wrap = new PdfPCell();
        wrap.setBorder(Rectangle.BOX);
        wrap.setBorderColor(BORDER);
        wrap.setBorderWidth(0.9f);
        wrap.setPadding(0f);
        wrap.setVerticalAlignment(Element.ALIGN_TOP);

        PdfPTable head = new PdfPTable(1);
        head.setWidthPercentage(100f);
        PdfPCell hc = new PdfPCell(new Paragraph(title, bold(10.5f, HEAD)));
        hc.setBackgroundColor(SLATE_50);
        hc.setBorder(Rectangle.BOTTOM);
        hc.setBorderColor(BORDER);
        hc.setBorderWidth(0.6f);
        hc.setPadding(7f);
        head.addCell(hc);
        wrap.addElement(head);

        PdfPTable bodyWrap = new PdfPTable(1);
        bodyWrap.setWidthPercentage(100f);
        PdfPCell bodyCell = new PdfPCell();
        bodyCell.setBorder(Rectangle.NO_BORDER);
        bodyCell.setPadding(8f);
        bodyCell.setPaddingTop(6f);
        bodyCell.setVerticalAlignment(Element.ALIGN_TOP);
        body.render(bodyCell);
        bodyWrap.addCell(bodyCell);
        wrap.addElement(bodyWrap);

        box.addCell(wrap);
        document.add(box);
    }

    @FunctionalInterface
    private interface BoxContent {
        void render(PdfPCell cell) throws DocumentException;
    }

    private void careerBody(PdfPCell cell, List<CareerResponse> items) throws DocumentException {
        if (items == null || items.isEmpty()) {
            return;
        }
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{34f, 33f, 33f});
        addTableHeader(table, "회사명", "직무", "기간");
        for (CareerResponse it : items) {
            addCareerRow(table, it);
        }
        cell.addElement(table);

        for (CareerResponse it : items) {
            if (hasText(it.description())) {
                Paragraph desc = new Paragraph(
                        "· " + normalizeNewlines(it.description()), regular(9.5f, BODY));
                desc.setLeading(11f);
                desc.setSpacingBefore(3f);
                cell.addElement(desc);
            }
        }
    }

    private void addCareerRow(PdfPTable table, CareerResponse item) {
        table.addCell(borderedData(new Paragraph(
                nullIfBlank(item.company()) == null ? " " : item.company(), bold(10f, BODY))));
        table.addCell(borderedData(new Paragraph(
                nullIfBlank(item.title()) == null ? " " : item.title(), regular(9.5f, BODY))));
        table.addCell(borderedData(new Paragraph(
                periodOrEmpty(item.startDate(), item.endDate()), regular(9.5f, MUTED))));
    }

    private void educationBody(PdfPCell cell, List<EducationResponse> items) throws DocumentException {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (EducationResponse it : items) {
            PdfPTable row = new PdfPTable(2);
            row.setWidthPercentage(100f);
            row.setWidths(new float[]{70f, 30f});
            row.setKeepTogether(true);

            PdfPCell mainCell = new PdfPCell();
            mainCell.setBorder(Rectangle.BOTTOM);
            mainCell.setBorderColor(RULE_LIGHT);
            mainCell.setBorderWidth(0.4f);
            mainCell.setPadding(3f);
            mainCell.setVerticalAlignment(Element.ALIGN_TOP);

            mainCell.addElement(new Paragraph(
                    it.school() == null ? " " : it.school(), bold(11f, BODY)));

            String sub = joinNonBlank(" · ",
                    nullIfBlank(it.major()),
                    hasText(it.degree()) ? it.degree() : null,
                    hasText(it.gpa()) ? "학점 " + it.gpa() : null,
                    nullIfBlank(it.status()));
            if (hasText(sub)) {
                mainCell.addElement(new Paragraph(sub, regular(9f, MUTED)));
            }

            PdfPCell periodCell = new PdfPCell(new Paragraph(
                    periodOrEmpty(it.startDate(), it.endDate()), regular(9f, FAINT)));
            periodCell.setBorder(Rectangle.BOTTOM);
            periodCell.setBorderColor(RULE_LIGHT);
            periodCell.setBorderWidth(0.4f);
            periodCell.setPadding(3f);
            periodCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            periodCell.setVerticalAlignment(Element.ALIGN_TOP);

            row.addCell(mainCell);
            row.addCell(periodCell);
            cell.addElement(row);
        }
    }

    private void skillBody(PdfPCell cell, List<SkillResponse> items) throws DocumentException {
        if (items == null || items.isEmpty()) {
            return;
        }
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{55f, 45f});
        addTableHeader(table, "스킬", "숙련도 / 분류");
        for (SkillResponse it : items) {
            table.addCell(borderedData(new Paragraph(
                    nullIfBlank(it.name()) == null ? " " : it.name(), regular(9.5f, BODY))));
            table.addCell(borderedData(new Paragraph(
                    joinNonBlank(" / ", nullIfBlank(it.level()), nullIfBlank(it.category())),
                    regular(9.5f, MUTED))));
        }
        cell.addElement(table);
    }

    private void certificateBody(PdfPCell cell, List<CertificateResponse> items) throws DocumentException {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (CertificateResponse it : items) {
            PdfPTable row = new PdfPTable(2);
            row.setWidthPercentage(100f);
            row.setWidths(new float[]{7f, 3f});
            row.setKeepTogether(true);

            PdfPCell nameCell = borderedData(new Paragraph(
                    nullIfBlank(it.name()) == null ? " " : it.name(), regular(10f, BODY)));
            PdfPCell dateCell = borderedData(new Paragraph(
                    it.acquiredAt() != null
                            ? it.acquiredAt().format(PdfLayoutSupport.YM) : "",
                    regular(9.5f, MUTED)));
            dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            row.addCell(nameCell);
            row.addCell(dateCell);
            cell.addElement(row);

            if (hasText(it.issuer())) {
                Paragraph issuer = new Paragraph(it.issuer(), regular(9f, MUTED));
                issuer.setSpacingBefore(2f);
                issuer.setSpacingAfter(5f);
                cell.addElement(issuer);
            }
        }
    }

    private void projectBody(PdfPCell cell, List<ProjectResponse> items) throws DocumentException {
        if (items == null || items.isEmpty()) {
            return;
        }
        boolean first = true;
        for (ProjectResponse it : items) {
            if (!hasProjectContent(it)) {
                continue;
            }
            if (!first) {
                cell.addElement(new Paragraph(new Chunk(
                        new LineSeparator(0.5f, 100f, BORDER, Element.ALIGN_LEFT, 0))));
            }
            first = false;

            PdfPTable head = new PdfPTable(2);
            head.setWidthPercentage(100f);
            head.setWidths(new float[]{7f, 3f});
            PdfPCell titleCell = borderedData(new Paragraph(
                    joinNonBlank(" · ", nullIfBlank(it.name()), nullIfBlank(it.role())),
                    bold(10.5f, HEAD)));
            PdfPCell periodCell = borderedData(new Paragraph(
                    periodOrEmpty(it.startDate(), it.endDate()), regular(9.5f, MUTED)));
            periodCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            head.addCell(titleCell);
            head.addCell(periodCell);
            cell.addElement(head);

            if (hasText(it.techStack())) {
                cell.addElement(new Paragraph(it.techStack(), regular(9f, MUTED)));
            }
            if (hasText(it.description())) {
                Paragraph desc = new Paragraph(normalizeNewlines(it.description()), regular(9.5f, BODY));
                desc.setLeading(11f);
                desc.setSpacingBefore(2f);
                cell.addElement(desc);
            }
            if (hasText(it.linkUrl())) {
                Paragraph link = new Paragraph("링크: " + it.linkUrl(), regular(9f, FAINT));
                link.setSpacingAfter(4f);
                cell.addElement(link);
            }
        }
    }

    private void introductionBody(PdfPCell cell, List<IntroductionResponse> items) throws DocumentException {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (IntroductionResponse it : items) {
            if (hasText(it.title())) {
                Paragraph title = new Paragraph(it.title(), bold(10.5f, HEAD));
                title.setSpacingAfter(3f);
                cell.addElement(title);
            }
            Paragraph body = new Paragraph(normalizeNewlines(it.content() == null ? "" : it.content()),
                    regular(9.5f, BODY));
            body.setLeading(11f);
            body.setSpacingAfter(8f);
            cell.addElement(body);
        }
    }

    private void portfolioBody(PdfPCell cell, List<PortfolioItemResponse> items) throws DocumentException {
        if (items == null || items.isEmpty()) {
            return;
        }
        boolean first = true;
        for (PortfolioItemResponse it : items) {
            if (!first) {
                cell.addElement(new Paragraph(new Chunk(
                        new LineSeparator(0.5f, 100f, BORDER, Element.ALIGN_LEFT, 0))));
            }
            first = false;

            String prefix = "FILE".equalsIgnoreCase(it.itemType()) ? "[파일] "
                    : "LINK".equalsIgnoreCase(it.itemType()) ? "[링크] " : "";
            Paragraph title = new Paragraph(prefix + (it.title() == null ? "" : it.title()),
                    bold(10.5f, HEAD));
            title.setSpacingAfter(3f);
            cell.addElement(title);

            if ("LINK".equalsIgnoreCase(it.itemType()) && hasText(it.linkUrl())) {
                cell.addElement(new Paragraph("링크: " + it.linkUrl(), regular(9f, FAINT)));
            }
            if (hasText(it.description())) {
                Paragraph desc = new Paragraph(normalizeNewlines(it.description()), regular(9.5f, BODY));
                desc.setLeading(11f);
                desc.setSpacingAfter(3f);
                cell.addElement(desc);
            }
        }
    }

    private void addTableHeader(PdfPTable table, String... markers) {
        for (String marker : markers) {
            PdfPCell cell = new PdfPCell(new Paragraph(marker, bold(8.5f, MUTED)));
            cell.setBackgroundColor(SLATE_50);
            cell.setBorder(Rectangle.BOTTOM);
            cell.setBorderColor(RULE_LIGHT);
            cell.setBorderWidth(0.6f);
            cell.setPadding(4f);
            table.addCell(cell);
        }
    }

    private PdfPCell borderedData(Paragraph paragraph) {
        PdfPCell cell = new PdfPCell(paragraph);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(RULE_LIGHT);
        cell.setBorderWidth(0.4f);
        cell.setPadding(4f);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        return cell;
    }

    /** 이름·역할·설명·기술스택·링크가 모두 비어 있으면 렌더에서 제외한다(빈 프로젝트 사각항목 방지). */
    private boolean hasProjectContent(ProjectResponse it) {
        return hasText(it.name()) || hasText(it.role()) || hasText(it.description())
                || hasText(it.techStack()) || hasText(it.linkUrl());
    }

    private Paragraph spacer(float height) {
        Paragraph spacer = new Paragraph(" ", regular(height, MUTED));
        spacer.setLeading(height);
        return spacer;
    }
}