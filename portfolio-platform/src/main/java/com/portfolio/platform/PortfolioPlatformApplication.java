package com.portfolio.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.portfolio.platform",
    "com.shplatform.common"
})
@EntityScan(basePackages = {
    "com.portfolio.platform.model",
    "com.shplatform.common.scheduling",
    "com.shplatform.common.notification"
})
@EnableJpaRepositories(basePackages = {
    "com.portfolio.platform.repository",
    "com.shplatform.common.scheduling",
    "com.shplatform.common.notification"
})
public class PortfolioPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioPlatformApplication.class, args);
    }
}
