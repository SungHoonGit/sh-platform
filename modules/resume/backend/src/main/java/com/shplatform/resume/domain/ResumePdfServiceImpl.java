package com.shplatform.resume.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
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
import com.shplatform.resume.api.dto.DocumentResponse;
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
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * OpenPDF 기반 이력서 PDF 생성 구현.
 *
 * <p>A4 크기 단일 서식으로 렌더링하며, 한글은 classpath {@code fonts/}의
 * Spoqa Han Sans (SIL OFL 1.1) TrueType을 임베드한다. 외부 프로세스/인프라 의존 없음.
 * 섹션 순서·포함 여부는 대상 문서의 sectionConfig를 따르고(지정 없으면 기본 편성),
 * 프로필 사진이 있으면 헤더 우측에 24mm로 배치한다.
 */
@Service
public class ResumePdfServiceImpl implements ResumePdfService {

    private static final float MARGIN_MM_18 = 51f;
    private static final float MARGIN_MM_16 = 45f;
    private static final float PHOTO_MM_24 = 68f;

    private static final Color TEXT = new Color(0x1F2937);
    private static final Color BODY = new Color(0x374151);
    private static final Color MUTED = new Color(0x6B7280);
    private static final Color ACCENT = new Color(0x0D9488);
    private static final Color RULE = new Color(0xE5E7EB);

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy.MM");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    /** 문서 편성 미지정 시 사용하는 기본 섹션 순서 (ResumeDocumentServiceImpl 기본값과 동일). */
    private static final List<String> DEFAULT_SECTION_ORDER = List.of(
            "careers", "projects", "educations", "skills",
            "certificates", "introductions", "portfolioItems");
    private static final Pattern PHOTO_URL_PATTERN = Pattern.compile("/files/(\\d+)/download");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ResumeViewService resumeViewService;
    private final ResumeDocumentService resumeDocumentService;
    private final FileStorageService fileStorageService;

    public ResumePdfServiceImpl(ResumeViewService resumeViewService,
                                ResumeDocumentService resumeDocumentService,
                                FileStorageService fileStorageService) {
        this.resumeViewService = resumeViewService;
        this.resumeDocumentService = resumeDocumentService;
        this.fileStorageService = fileStorageService;
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
    public byte[] generatePdf(Long userId, Long documentId) {
        ResumeViewResponse view = resumeViewService.getMyResumeView(userId);
        List<String> sectionKeys = resolveSectionOrder(userId, documentId);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             Document document = new Document(PageSize.A4, MARGIN_MM_18, MARGIN_MM_18, MARGIN_MM_16, MARGIN_MM_16)) {
            PdfWriter.getInstance(document, bos);
            document.open();

            renderHeader(document, view, userId);

            for (String key : sectionKeys) {
                renderSection(document, view, key);
            }

            document.close();
            return bos.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("이력서 PDF 생성 실패: userId=" + userId, e);
        }
    }

    private void renderHeader(Document document, ResumeViewResponse view, Long userId) throws DocumentException {
        ProfileResponse profile = view.profile();
        if (profile == null) {
            Paragraph spacer = new Paragraph(" ", font(Fonts.REGULAR, 1f, TEXT));
            spacer.setLeading(1f);
            document.add(spacer);
            return;
        }

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100f);
        header.setWidths(new float[]{7f, 3f});
        header.setKeepTogether(true);

        String name = hasText(profile.name()) ? profile.name() : "(이름 미등록)";
        Paragraph nameP = new Paragraph(name, font(Fonts.BOLD, 26f, TEXT));
        nameP.setSpacingAfter(4f);

        PdfPCell left = textCell();
        left.addElement(nameP);

        if (hasText(profile.headline())) {
            Paragraph headline = new Paragraph(profile.headline(), font(Fonts.REGULAR, 11.5f, MUTED));
            headline.setSpacingAfter(4f);
            left.addElement(headline);
        }

        String contact = joinNonBlank("  ·  ",
                nullIfBlank(profile.email()),
                nullIfBlank(profile.phone()),
                nullIfBlank(profile.address()),
                profile.birthDate() != null ? profile.birthDate().format(YMD) : null);
        if (hasText(contact)) {
            Paragraph contactP = new Paragraph(contact, font(Fonts.REGULAR, 10f, MUTED));
            contactP.setSpacingAfter(8f);
            left.addElement(contactP);
        }
        header.addCell(left);

        Image photo = loadPhoto(profile, userId);
        if (photo != null) {
            PdfPCell photoCell = new PdfPCell(photo);
            photoCell.setBorder(Rectangle.NO_BORDER);
            photoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            photoCell.setVerticalAlignment(Element.ALIGN_TOP);
            photoCell.setPaddingLeft(8f);
            header.addCell(photoCell);
        } else {
            header.addCell(textCell());
        }

        document.add(header);
        document.add(new Chunk(new LineSeparator(0.8f, 100f, ACCENT, Element.ALIGN_LEFT, 4)));
    }

    private void renderSection(Document document, ResumeViewResponse view, String key) throws DocumentException {
        switch (key) {
            case "careers" -> renderGroup(document, "경력", view.careers(), (d, it) -> renderCareer(d, it));
            case "projects" -> renderGroup(document, "프로젝트", view.projects(), (d, it) -> renderProject(d, it));
            case "educations" -> renderGroup(document, "학력", view.educations(), (d, it) -> renderEducation(d, it));
            case "skills" -> renderGroup(document, "스킬", view.skills(), (d, it) -> renderSkill(d, it));
            case "certificates" -> renderGroup(document, "자격증", view.certificates(), (d, it) -> renderCertificate(d, it));
            case "introductions" -> renderGroup(document, "자기소개", view.introductions(), (d, it) -> renderIntroduction(d, it));
            case "portfolioItems" -> renderGroup(document, "포트폴리오", view.portfolioItems(), (d, it) -> renderPortfolio(d, it));
            default -> { /* 알 수 없는 key는 무시 */ }
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

    /** 문서 ID가 있으면 해당 문서의 sectionConfig로, 없으면 기본 편성 순서로 섹션 key를 결정한다. */
    private List<String> resolveSectionOrder(Long userId, Long documentId) {
        if (documentId == null) {
            return DEFAULT_SECTION_ORDER;
        }
        String config = resumeDocumentService.getDocuments(userId).stream()
                .filter(doc -> doc.id().equals(documentId))
                .map(DocumentResponse::sectionConfig)
                .findFirst()
                .orElse(null);
        if (config == null) {
            return DEFAULT_SECTION_ORDER;
        }
        List<String> keys = parseSectionKeys(config);
        return keys;
    }

    private List<String> parseSectionKeys(String sectionConfig) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(sectionConfig);
            if (root == null || !root.isArray()) {
                return DEFAULT_SECTION_ORDER;
            }
            List<JsonNode> included = new ArrayList<>();
            for (JsonNode node : root) {
                if (node.has("included") && node.get("included").asBoolean(false)) {
                    included.add(node);
                }
            }
            included.sort(Comparator.comparingInt(
                    node -> node.has("order") ? node.get("order").asInt(Integer.MAX_VALUE) : Integer.MAX_VALUE));
            List<String> keys = included.stream()
                    .map(node -> node.has("key") ? node.get("key").asText("") : "")
                    .filter(key -> !key.isBlank())
                    .toList();
            return keys.isEmpty() ? DEFAULT_SECTION_ORDER : keys;
        } catch (Exception e) {
            return DEFAULT_SECTION_ORDER;
        }
    }

    /** 프로필 사진을 서버 디스크에서 읽어 24mm로 스케일한다. 없거나 깨졌으면 null (feat: 사진 선택). */
    private Image loadPhoto(ProfileResponse profile, Long userId) {
        try {
            Long fileId = parseFileId(profile.photoUrl());
            if (fileId == null) {
                return null;
            }
            var file = fileStorageService.download(userId, fileId);
            Image image = Image.getInstance(file.data());
            image.scaleToFit(PHOTO_MM_24, PHOTO_MM_24);
            return image;
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseFileId(String photoUrl) {
        if (photoUrl == null) {
            return null;
        }
        Matcher matcher = PHOTO_URL_PATTERN.matcher(photoUrl);
        return matcher.find() ? Long.valueOf(matcher.group(1)) : null;
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
        if (hasText(item.title())) {
            Paragraph title = new Paragraph(item.title(), font(Fonts.BOLD, 11.5f, TEXT));
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

    private PdfPCell textCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0f);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        return cell;
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
}