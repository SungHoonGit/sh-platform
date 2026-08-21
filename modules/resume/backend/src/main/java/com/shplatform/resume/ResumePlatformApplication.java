package com.shplatform.resume;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.shplatform.resume",
    "com.shplatform.common"
})
@EntityScan(basePackages = {
    "com.shplatform.resume",
    "com.shplatform.common.scheduling",
    "com.shplatform.common.notification"
})
@EnableJpaRepositories(basePackages = {
    "com.shplatform.resume",
    "com.shplatform.common.scheduling",
    "com.shplatform.common.notification"
})
public class ResumePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumePlatformApplication.class, args);
    }
}
