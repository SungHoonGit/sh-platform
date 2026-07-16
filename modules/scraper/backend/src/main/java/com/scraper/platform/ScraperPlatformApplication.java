package com.scraper.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.scraper.platform",
    "com.shplatform.common"
})
@EntityScan(basePackages = {
    "com.scraper.platform.model",
    "com.shplatform.common.scheduling",
    "com.shplatform.common.notification"
})
@EnableJpaRepositories(basePackages = {
    "com.scraper.platform.repository",
    "com.shplatform.common.scheduling",
    "com.shplatform.common.notification"
})
@EnableAsync
public class ScraperPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScraperPlatformApplication.class, args);
    }
}
