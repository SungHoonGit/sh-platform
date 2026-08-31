package com.shplatform.resume.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Content-Disposition 헤더 생성 유틸리티.
 *
 * <p>Spring의 {@code ContentDisposition}는 비ASCII 파일명에 RFC 2047 부호문
 * ({@code =?UTF-8?Q?...?=})을 {@code filename}으로 내보내 일부 브라우저/OS에서
 * 깨진 파일명으로 저장된다. 여기서는 ASCII 안전 폴백 + RFC 5987 {@code filename*}을 사용한다.</p>
 */
public final class ContentDispositionSupport {

    private ContentDispositionSupport() {
    }

    /**
     * 다운로드 헤더 값을 만든다. {@code filename}을 UTF-8 퍼센트 인코딩한
     * {@code filename*}, 비ASCII를 밑줄로 치환한 ASCII 안전 {@code filename} 폴백을 함께 담는다.
     *
     * @param filename 저장될 파일명 (비ASCII 허용)
     * @return "attachment; filename=...; filename*=UTF-8''..." 형식 헤더 값
     */
    public static String attachment(String filename) {
        String ascii = toAsciiFallback(filename);
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }

    private static String toAsciiFallback(String filename) {
        String safe = filename == null ? "" : filename;
        safe = safe.replaceAll("[^\\x20-\\x7E\\x80-\\xFF]", "_");
        safe = safe.replaceAll("[\"\\\\]", "_");
        return safe.isBlank() ? "download" : safe;
    }
}