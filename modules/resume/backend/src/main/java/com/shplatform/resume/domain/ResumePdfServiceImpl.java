package com.shplatform.resume.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import com.shplatform.resume.api.dto.DocumentResponse;
import com.shplatform.resume.api.dto.ResumeViewResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * OpenPDF 기반 이력서 PDF 생성 구현.
 *
 * <p>A4 크기로 렌더링하며, 한글은 classpath {@code fonts/}의
 * Spoqa Han Sans (SIL OFL 1.1) TrueType을 임베드한다. 외부 프로세스/인프라 의존 없음.
 * 대상 문서가 있으면 문서의 {@code templateCode}로 테마 레이아웃
 * ({@link ClassicPdfLayout}/{@link ModernPdfLayout}/{@link SaraminPdfLayout})을 선택하고
 * {@code sectionConfig}(섹션 포함·순서)를 적용한다. 문서가 없으면 클래식 기본 편성으로 생성한다.
 */
@Service
public class ResumePdfServiceImpl implements ResumePdfService {

    private static final float MARGIN_MM_18 = 51f;
    private static final float MARGIN_MM_16 = 45f;

    /** 문서 편성 미지정 시 사용하는 기본 섹션 순서 (ResumeDocumentServiceImpl 기본값과 동일). */
    private static final List<String> DEFAULT_SECTION_ORDER = List.of(
            "careers", "projects", "educations", "skills",
            "certificates", "introductions", "portfolioItems");
    private static final String DEFAULT_TEMPLATE_CODE = "CLASSIC";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ResumeViewService resumeViewService;
    private final ResumeDocumentService resumeDocumentService;
    private final Map<String, ResumePdfLayout> layouts;

    public ResumePdfServiceImpl(ResumeViewService resumeViewService,
                                ResumeDocumentService resumeDocumentService,
                                ClassicPdfLayout classicLayout,
                                ModernPdfLayout modernLayout,
                                SaraminPdfLayout saraminLayout) {
        this.resumeViewService = resumeViewService;
        this.resumeDocumentService = resumeDocumentService;
        Map<String, ResumePdfLayout> map = new HashMap<>();
        map.put("CLASSIC", classicLayout);
        map.put("MODERN", modernLayout);
        map.put("SARAMIN", saraminLayout);
        this.layouts = Map.copyOf(map);
    }

    @Override
    public byte[] generatePdf(Long userId, Long documentId) {
        ResumeViewResponse view = resumeViewService.getMyResumeView(userId);
        DocumentOption option = resolveDocumentOption(userId, documentId);
        ResumePdfLayout layout = layouts.getOrDefault(option.templateCode(), layouts.get(DEFAULT_TEMPLATE_CODE));
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             Document document = new Document(PageSize.A4, MARGIN_MM_18, MARGIN_MM_18, MARGIN_MM_16, MARGIN_MM_16)) {
            PdfWriter.getInstance(document, bos);
            document.open();
            layout.render(document, view, option.sectionKeys(), userId);
            document.close();
            return bos.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("이력서 PDF 생성 실패: userId=" + userId, e);
        }
    }

    private record DocumentOption(List<String> sectionKeys, String templateCode) {
    }

    /** 문서 ID가 있으면 해당 문서의 sectionConfig/templateCode를 사용하고, 없으면 기본값을 쓴다. */
    private DocumentOption resolveDocumentOption(Long userId, Long documentId) {
        if (documentId == null) {
            return new DocumentOption(DEFAULT_SECTION_ORDER, DEFAULT_TEMPLATE_CODE);
        }
        DocumentResponse doc = resumeDocumentService.getDocuments(userId).stream()
                .filter(d -> d.id().equals(documentId))
                .findFirst()
                .orElse(null);
        if (doc == null) {
            return new DocumentOption(DEFAULT_SECTION_ORDER, DEFAULT_TEMPLATE_CODE);
        }
        List<String> keys = parseSectionKeys(doc.sectionConfig());
        String template = (doc.templateCode() == null || doc.templateCode().isBlank())
                ? DEFAULT_TEMPLATE_CODE : doc.templateCode().toUpperCase();
        return new DocumentOption(keys, template);
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
}