package com.scraper.platform.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CrawlExecutionService cron 판정 테스트")
class CrawlExecutionServiceTest {

    // 2026-09-17은 목요일
    private static final LocalDateTime THU_0900 = LocalDateTime.of(2026, 9, 17, 9, 0);
    private static final LocalDateTime THU_0859 = LocalDateTime.of(2026, 9, 17, 8, 59);
    private static final LocalDateTime SAT_0900 = LocalDateTime.of(2026, 9, 19, 9, 0);

    @Nested
    @DisplayName("isDue 메서드")
    class IsDue {

        @Test
        @DisplayName("매일 09:00 스케줄은 09:00에 발화한다")
        void daily_fire() {
            assertTrue(CrawlExecutionService.isDue("0 9 * * *", THU_0900, null));
        }

        @Test
        @DisplayName("매일 09:00 스케줄은 08:59에 발화하지 않는다")
        void daily_noFire() {
            assertFalse(CrawlExecutionService.isDue("0 9 * * *", THU_0859, null));
        }

        @Test
        @DisplayName("평일 스케줄은 토요일에 발화하지 않는다")
        void weekday_saturday() {
            assertFalse(CrawlExecutionService.isDue("0 9 * * 1-5", SAT_0900, null));
        }

        @Test
        @DisplayName("평일 스케줄은 목요일에 발화한다")
        void weekday_thursday() {
            assertTrue(CrawlExecutionService.isDue("0 9 * * 1-5", THU_0900, null));
        }

        @Test
        @DisplayName("같은 발화 시각에 두 번 호출하면 두 번째는 거짓이다 (중복 방지)")
        void duplicate_suppressed() {
            LocalDateTime firstFire = THU_0900;
            assertTrue(CrawlExecutionService.isDue("0 9 * * *", firstFire, null));
            assertFalse(CrawlExecutionService.isDue("0 9 * * *", firstFire, firstFire));
        }

        @Test
        @DisplayName("잘못된 cron은 거짓이다")
        void invalid_cron() {
            assertFalse(CrawlExecutionService.isDue("not a cron", THU_0900, null));
        }

        @Test
        @DisplayName("분 단위 스케줄은 해당 분에 발화한다")
        void minutely() {
            LocalDateTime t = LocalDateTime.of(2026, 9, 17, 9, 30);
            assertTrue(CrawlExecutionService.isDue("30 9 * * *", t, null));
            assertFalse(CrawlExecutionService.isDue("31 9 * * *", t, null));
        }
    }
}
