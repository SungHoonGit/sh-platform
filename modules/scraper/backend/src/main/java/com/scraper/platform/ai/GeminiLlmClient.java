package com.scraper.platform.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class GeminiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean supports(String provider) {
        return "gemini".equalsIgnoreCase(provider);
    }

    @Override
    public String generate(String prompt, LlmProperties properties) {
        try {
            String url = properties.resolvedBaseUrl().replaceAll("/+$", "")
                    + "/v1beta/models/" + properties.model() + ":generateContent?key=" + properties.apiKey();
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", prompt))
                    )),
                    "generationConfig", Map.of(
                            "temperature", 0.2,
                            "maxOutputTokens", 1200
                    )
            );
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(properties.resolvedTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(Math.min(10, properties.resolvedTimeoutSeconds())))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Gemini API failed: status={}, body={}", response.statusCode(), truncate(response.body()));
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
            JsonNode root = MAPPER.readTree(response.body());
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            if (text.isBlank()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
            return text.trim();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gemini API call error: {}", e.toString());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String truncate(String value) {
        if (value == null) return "";
        return value.length() > 300 ? value.substring(0, 300) + "..." : value;
    }
}
