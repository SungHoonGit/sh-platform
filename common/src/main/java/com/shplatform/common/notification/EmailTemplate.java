package com.shplatform.common.notification;

/**
 * 이메일 HTML 공통 프레임 템플릿.
 *
 * <p>SH Platform 사이트 디자인(그라디언트 헤더 + 화이트 카드 + 정보 푸터)에 맞춘
 * 공통 레이아웃을 제공한다. 각 모듈은 본문 영역만 조립해서 전달한다.
 *
 * <p>사용 예:
 * <pre>{@code
 * String body = "<p>안녕하세요.</p>" + EmailTemplate.codeBox("123456");
 * String html = EmailTemplate.frame("이메일 인증 코드", "SH Platform 회원 인증",
 *         body, "회원가입 인증 요청에 의해 발송되었습니다.");
 * }</pre>
 */
public final class EmailTemplate {

    /** 브랜드 그라디언트 헤더 색상 (blue-900 → blue-600) */
    private static final String HEADER_GRADIENT = "linear-gradient(135deg,#1e3a8a,#2563eb)";

    /** 서비스 대표 URL */
    private static final String SITE_URL = "https://sunghoonyk.duckdns.org";

    private EmailTemplate() {
    }

    /**
     * 공통 프레임으로 전체 HTML 문서를 생성한다. (기본 푸터)
     *
     * @param title    헤더 제목
     * @param subtitle 헤더 부제목 (설정 이름, 서비스 구분 등)
     * @param bodyHtml 본문 HTML (프레임 내부 컨텐츠)
     * @return 완성된 HTML 문서
     */
    public static String frame(String title, String subtitle, String bodyHtml) {
        return frame(title, subtitle, bodyHtml, null);
    }

    /**
     * 공통 프레임으로 전체 HTML 문서를 생성한다.
     *
     * @param title        헤더 제목
     * @param subtitle     헤더 부제목 (설정 이름, 서비스 구분 등)
     * @param bodyHtml     본문 HTML (프레임 내부 컨텐츠)
     * @param footerReason 푸터 발송 사유 문구 (null이면 생략)
     * @return 완성된 HTML 문서
     */
    public static String frame(String title, String subtitle, String bodyHtml, String footerReason) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><body style=\"margin:0;padding:32px 16px;background-color:#f3f4f6;")
                .append("font-family:-apple-system,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;\">");
        html.append("<div style=\"max-width:600px;margin:0 auto;background-color:#ffffff;")
                .append("border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,0.08);\">");

        // 헤더
        html.append("<div style=\"background:").append(HEADER_GRADIENT)
                .append(";padding:32px 40px;color:#ffffff;\">");
        html.append("<h2 style=\"margin:0;font-size:21px;font-weight:700;letter-spacing:-0.3px;\">")
                .append(escapeHtml(title)).append("</h2>");
        if (subtitle != null && !subtitle.isBlank()) {
            html.append("<p style=\"margin:8px 0 0;font-size:14px;color:#bfdbfe;\">")
                    .append(escapeHtml(subtitle)).append("</p>");
        }
        html.append("</div>");

        // 본문
        html.append("<div style=\"padding:36px 40px;\">").append(bodyHtml).append("</div>");

        // 푸터 (발송 사유 + 설정 링크 + 사이트 정보)
        html.append("<div style=\"padding:24px 40px;background:#f8fafc;border-top:1px solid #e2e8f0;\">");
        if (footerReason != null && !footerReason.isBlank()) {
            html.append("<p style=\"margin:0 0 12px;font-size:12px;color:#64748b;line-height:1.7;\">")
                    .append(escapeHtml(footerReason)).append("</p>");
        }
        html.append("<p style=\"margin:0 0 14px;font-size:12px;\">")
                .append("<a href=\"").append(SITE_URL).append("/platform/settings\"")
                .append(" style=\"color:#2563eb;text-decoration:none;\">알림 설정 변경</a>")
                .append("</p>");
        html.append("<p style=\"margin:0;font-size:11px;color:#94a3b8;\">")
                .append("SH Platform · ").append(SITE_URL.replace("https://", ""))
                .append("</p>");
        html.append("</div>");

        html.append("</div></body></html>");
        return html.toString();
    }

    /**
     * 강조 코드 박스를 생성한다. (인증 코드 등)
     *
     * @param code 표시할 코드
     * @return 코드 박스 HTML
     */
    public static String codeBox(String code) {
        return "<div style=\"text-align:center;background:#eff6ff;border:1px solid #dbeafe;"
                + "border-radius:8px;padding:26px;margin:0 0 24px;\">"
                + "<div style=\"font-size:34px;font-weight:700;color:#2563eb;letter-spacing:10px;"
                + "font-family:'Courier New',monospace;\">" + code + "</div>"
                + "</div>";
    }

    /**
     * 요약 통계 카드 행을 생성한다. (1~3개)
     *
     * @param stats 값/라벨 쌍 배열 (값, 라벨, 순서대로)
     * @return 카드 행 HTML
     */
    public static String statCards(String... stats) {
        if (stats.length % 2 != 0) {
            throw new IllegalArgumentException("stats must be value/label pairs");
        }
        String[] backgrounds = {"#eff6ff|#dbeafe|#2563eb", "#f0fdf4|#dcfce7|#16a34a", "#f8fafc|#e2e8f0|#64748b"};
        StringBuilder html = new StringBuilder();
        html.append("<div style=\"display:flex;gap:12px;margin-bottom:24px;\">");
        for (int i = 0; i < stats.length; i += 2) {
            String[] color = backgrounds[(i / 2) % backgrounds.length].split("\\|");
            html.append("<div style=\"flex:1;text-align:center;background:").append(color[0])
                    .append(";border-radius:8px;padding:16px;border:1px solid ").append(color[1]).append(";\">");
            html.append("<div style=\"font-size:24px;font-weight:700;color:").append(color[2])
                    .append(";\">").append(stats[i]).append("</div>");
            html.append("<div style=\"font-size:12px;color:#64748b;margin-top:6px;\">").append(stats[i + 1]).append("</div>");
            html.append("</div>");
        }
        html.append("</div>");
        return html.toString();
    }

    /**
     * CTA 버튼을 생성한다.
     *
     * @param url  이동할 URL
     * @param text 버튼 문구
     * @return 버튼 HTML
     */
    public static String button(String url, String text) {
        return "<div style=\"margin-top:28px;margin-bottom:8px;text-align:center;\">"
                + "<a href=\"" + url + "\" style=\"display:inline-block;background:#2563eb;color:#ffffff;"
                + "text-decoration:none;padding:13px 30px;border-radius:8px;font-size:14px;font-weight:600;\">"
                + escapeHtml(text) + "</a>"
                + "</div>";
    }

    /**
     * HTML 특수문자를 이스케이프한다.
     *
     * @param value 원본 문자열
     * @return 이스케이프된 문자열
     */
    public static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
