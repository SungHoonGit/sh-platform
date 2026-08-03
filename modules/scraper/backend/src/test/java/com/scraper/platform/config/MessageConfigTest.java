package com.scraper.platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class MessageConfigTest {

    private final MessageConfig config = new MessageConfig();

    @Test
    void resolvesKoreanMessageFromBundle() {
        assertThat(config.messageSource().getMessage("DUPLICATE_NAME", null, Locale.KOREAN))
                .isEqualTo("이미 같은 이름의 스케줄이 있습니다. 다른 이름을 사용해 주세요.");
    }

    @Test
    void resolvesEnglishMessageFromBundle() {
        assertThat(config.messageSource().getMessage("INTERNAL_ERROR", null, Locale.ENGLISH))
                .isEqualTo("An internal server error occurred. Please try again later.");
    }

    @Test
    void unknownCodeFallsBackToCode() {
        assertThat(config.messageSource().getMessage("UNKNOWN_CODE", null, Locale.KOREAN))
                .isEqualTo("UNKNOWN_CODE");
    }
}
