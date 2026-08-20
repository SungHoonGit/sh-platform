package com.scraper.platform.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * 중복 공고(dedup_key) 유니크 제약 위반 ERROR 로그를 필터링한다.
 * <p>
 * 크롤링 시 동일 공고가 여러 config에서 수집되거나 동시 실행되는 경우
 * {@code uk_job_postings_dedup} 예외가 발생하는데, 이는 비즈니스적으로 정상 중복이며
 * {@code CrawlExecutionService#saveJobPostings}에서 이미 dup로 처리한다.
 * Hibernate {@code SqlExceptionHelper}가 ERROR 레벨로 먼저 기록하므로 모니터링 오염을 막기 위해 제거한다.
 */
public class DuplicateJobErrorFilter extends Filter<ILoggingEvent> {

    private static final String TARGET = "uk_job_postings_dedup";

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event.getLevel().isGreaterOrEqual(Level.ERROR)
                && event.getFormattedMessage() != null
                && event.getFormattedMessage().contains(TARGET)) {
            return FilterReply.DENY;
        }
        return FilterReply.NEUTRAL;
    }
}