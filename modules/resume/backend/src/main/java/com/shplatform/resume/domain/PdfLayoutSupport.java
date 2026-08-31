package com.shplatform.resume.domain;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfWriter;
import com.shplatform.resume.api.dto.ProfileResponse;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF 레이아웃 공용 지원 — 폰트(정적 로드), 색, 날짜 포맷, 사진 로드, 페이지 공간, 문자열 유틸.
 * 테마 레이아웃 3종(클래식/모던/사람인형)이 같은 패키지에서 사용한다.
 */
final class PdfLayoutSupport {

    static final float MARGIN_MM_18 = 51f;
    static final float MARGIN_MM_16 = 45f;

    /** 사진 표준 프레임 (24×32mm, 모든 테마 공통). */
    static final float PHOTO_W = 68f;
    static final float PHOTO_H = 90.6f;

    /** 섹션 시작 시 남은 공간이 이 값(포인트)보다 작으면 다음 페이지로 보낸다. */
    static final float MIN_SECTION_SPACE = 70f;

    static final Color INK = new Color(0x0F172A);
    static final Color HEAD = new Color(0x1E293B);
    static final Color BODY = new Color(0x475569);
    static final Color MUTED = new Color(0x64748B);
    static final Color FAINT = new Color(0x94A3B8);
    static final Color MUTE_LIGHT = new Color(0x6B7280);
    static final Color WHITE = new Color(0xFFFFFF);
    static final Color ON_DARK_MUTED = new Color(0xCBD5E1);
    static final Color TEAL = new Color(0x14B8A6);
    static final Color TEAL_DARK = new Color(0x0F766E);
    static final Color SLATE_BG = new Color(0x1E293B);
    static final Color SLATE_SUB = new Color(0x334155);
    static final Color SLATE_50 = new Color(0xF8FAFC);
    static final Color BORDER = new Color(0xD1D5DB);
    static final Color RULE_LIGHT = new Color(0xE5E7EB);

    static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy.MM");
    static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    static final Pattern PHOTO_URL_PATTERN = Pattern.compile("/files/(\\d+)/download");

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
            } catch (IOException | com.lowagie.text.DocumentException e) {
                throw new IllegalStateException("폰트 로드 실패: " + path, e);
            }
        }
    }

    private PdfLayoutSupport() {
    }

    static Font font(BaseFont baseFont, float size, Color color) {
        return new Font(baseFont, size, Font.NORMAL, color);
    }

    static Font regular(float size, Color color) {
        return font(Fonts.REGULAR, size, color);
    }

    static Font bold(float size, Color color) {
        return font(Fonts.BOLD, size, color);
    }

    /** 프로필 사진을 디스크에서 읽어 표준 프레임(24×32mm)에 맞춘다. 없거나 손상됐으면 null (설계: 사진 선택). */
    static Image loadPhoto(ProfileResponse profile, Long userId, FileStorageService fileStorage) {
        return loadPhoto(profile, userId, fileStorage, PHOTO_W, PHOTO_H);
    }

    /** 프로필 사진을 디스크에서 읽어 지정 박스에 맞춘다. 없거나 손상됐으면 null (설계: 사진 선택). */
    static Image loadPhoto(ProfileResponse profile, Long userId, FileStorageService fileStorage,
                           float maxWidthPt, float maxHeightPt) {
        if (profile == null || profile.photoUrl() == null) {
            return null;
        }
        try {
            Matcher matcher = PHOTO_URL_PATTERN.matcher(profile.photoUrl());
            if (!matcher.find()) {
                return null;
            }
            Long fileId = Long.valueOf(matcher.group(1));
            var downloaded = fileStorage.download(userId, fileId);
            Image image = Image.getInstance(downloaded.data());
            image.scaleToFit(maxWidthPt, maxHeightPt);
            return image;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 모든 테마에 동일한 사진 프레임 셀을 만든다 (0.8pt 회색 테두리 + 2pt 패딩, 중앙 정렬).
     * 사진이 없으면 null을 반환한다.
     */
    static PdfPCell photoCell(Image photo) {
        if (photo == null) {
            return null;
        }
        PdfPCell cell = new PdfPCell(photo);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(0.8f);
        cell.setBorderColor(BORDER);
        cell.setPadding(2f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    /**
     * 섹션(제목+첫 줄)을 새로 시작할 공간이 부족하면 다음 페이지로 보낸다.
     * 이렇게 하면 페이지 끝에 제목만 남거나 내용이 잘려 보이는 고아(orphan)를 막는다.
     */
    static void ensureRoom(Document document, PdfWriter writer, float neededPt) throws DocumentException {
        float remaining = writer.getVerticalPosition(true) - document.bottomMargin();
        if (remaining < neededPt) {
            document.newPage();
        }
    }

    static String periodOrEmpty(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return "";
        }
        return period(start, end);
    }

    static String period(LocalDate start, LocalDate end) {
        String from = start != null ? start.format(YM) : "";
        String to = end != null ? end.format(YM) : "현재";
        return (from.isEmpty() ? "" : from + " – ") + to;
    }

    static String normalizeNewlines(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    static String nullIfBlank(String value) {
        return hasText(value) ? value : null;
    }

    static String joinNonBlank(String separator, String... parts) {
        List<String> kept = new ArrayList<>();
        for (String part : parts) {
            if (hasText(part)) {
                kept.add(part);
            }
        }
        return String.join(separator, kept);
    }
}