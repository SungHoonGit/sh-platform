package com.scraper.platform.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        String provider,
        String model,
        String apiKey,
        String baseUrl,
        Integer timeoutSeconds
) {
    public boolean configured() {
        return provider != null && !provider.isBlank()
                && model != null && !model.isBlank()
                && apiKey != null && !apiKey.isBlank();
    }

    public String resolvedBaseUrl() {
        return (baseUrl == null || baseUrl.isBlank())
                ? "https://generativelanguage.googleapis.com"
                : baseUrl;
    }

    public int resolvedTimeoutSeconds() {
        return (timeoutSeconds == null || timeoutSeconds < 5) ? 45 : timeoutSeconds;
    }
}
