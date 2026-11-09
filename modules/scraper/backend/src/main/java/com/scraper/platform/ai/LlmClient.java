package com.scraper.platform.ai;

public interface LlmClient {
    boolean supports(String provider);

    String generate(String prompt, LlmProperties properties);
}
