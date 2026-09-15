package com.scraper.platform.crawler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("HttpFetcher 응답 디코딩 테스트")
class HttpFetcherTest {

    private byte[] gzip(String text) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(bos)) {
            gz.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return bos.toByteArray();
    }

    private byte[] deflate(String text) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DeflaterOutputStream def = new DeflaterOutputStream(bos)) {
            def.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return bos.toByteArray();
    }

    @Nested
    @DisplayName("decode 메서드")
    class Decode {

        @Test
        @DisplayName("gzip 응답을 UTF-8 텍스트로 해제한다")
        void gzip_해제() throws IOException {
            assertEquals("{\"count\":1}", HttpFetcher.decode(gzip("{\"count\":1}"), "gzip"));
        }

        @Test
        @DisplayName("Content-Encoding 대소문자와 x-gzip 별칭을 허용한다")
        void gzip_별칭_허용() throws IOException {
            assertEquals("abc", HttpFetcher.decode(gzip("abc"), "GZip"));
            assertEquals("abc", HttpFetcher.decode(gzip("abc"), "x-gzip"));
        }

        @Test
        @DisplayName("deflate 응답을 해제한다")
        void deflate_해제() throws IOException {
            assertEquals("abc", HttpFetcher.decode(deflate("abc"), "deflate"));
        }

        @Test
        @DisplayName("압축되지 않은 응답은 원본 UTF-8 문자열을 그대로 반환한다")
        void 비압축_원본() throws IOException {
            assertEquals("한글 JSON", HttpFetcher.decode("한글 JSON".getBytes(StandardCharsets.UTF_8), ""));
        }

        @Test
        @DisplayName("null 바디는 빈 문자열을 반환한다")
        void null_바디() throws IOException {
            assertEquals("", HttpFetcher.decode(null, "gzip"));
        }
    }
}