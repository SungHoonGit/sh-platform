package com.scraper.platform.crawler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * 채용사이트 HTTP 호출 공통 유틸.
 * <p>
 * 예전 curl {@code --compressed} 와 동일하게 gzip/deflate 응답을 자동 해제한다.
 * (curl 대신 Java HttpClient 사용시 BodyHandlers.ofString() 은 gzip 을 해제하지 않으므로
 * Content-Encoding 을 보고 직접 풀어야 한다 — 오류 기록 015 참고)
 */
final class HttpFetcher {

    private HttpFetcher() {
    }

    /**
     * 30초 타임아웃, 리다이렉트 따라가기를 기본으로 HTTP 요청을 보내고 원시 바이트 응답을 반환한다.
     */
    static HttpResponse<byte[]> send(HttpRequest request) throws IOException, InterruptedException {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
                .send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    /**
     * 응답의 Content-Encoding 에 따라 gzip/deflate 를 해제하고 UTF-8 문자열로 변환한다.
     */
    static String decodeBody(HttpResponse<byte[]> response) throws IOException {
        String encoding = response.headers()
                .firstValue("Content-Encoding").orElse("").trim().toLowerCase();
        return decode(response.body(), encoding);
    }

    /**
     * 인코딩(대소문자 무관)에 따라 바이트를 해제해 UTF-8 문자열로 변환한다.
     * 알 수 없는 인코딩이거나 null 바디면 원본을 그대로 사용한다.
     */
    static String decode(byte[] body, String contentEncoding) throws IOException {
        if (body == null || body.length == 0) {
            return "";
        }
        String encoding = contentEncoding == null ? "" : contentEncoding.trim().toLowerCase();
        return switch (encoding) {
            case "gzip", "x-gzip" -> new String(gunzip(body), StandardCharsets.UTF_8);
            case "deflate" -> new String(inflate(body), StandardCharsets.UTF_8);
            default -> new String(body, StandardCharsets.UTF_8);
        };
    }

    private static byte[] gunzip(byte[] body) throws IOException {
        return new GZIPInputStream(new ByteArrayInputStream(body)).readAllBytes();
    }

    private static byte[] inflate(byte[] body) throws IOException {
        return new InflaterInputStream(new ByteArrayInputStream(body)).readAllBytes();
    }
}