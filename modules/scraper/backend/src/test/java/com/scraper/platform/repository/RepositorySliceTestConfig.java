package com.scraper.platform.repository;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA 슬라이스 전용 설정 (설계 036 게이트 테스트).
 * 실제 ScraperPlatformApplication(@ComponentScan 전체) 대신 이 설정이
 * 가장 가까운 패키지에서 @SpringBootConfiguration로 발견되어,
 * 컨트롤러/서비스(Executor·Mail 등) 없이 엔티티+리포지토리만 로드한다.
 */
@SpringBootConfiguration
@EnableJpaRepositories(basePackages = {
        "com.scraper.platform.repository",
        "com.shplatform.common.scheduling",
        "com.shplatform.common.notification"
})
@EntityScan(basePackages = {
        "com.scraper.platform.model",
        "com.shplatform.common.scheduling",
        "com.shplatform.common.notification"
})
public class RepositorySliceTestConfig {
}
