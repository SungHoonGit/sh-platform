package com.shplatform.common.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ComponentScan(basePackages = "com.shplatform.common.notification")
@RequiredArgsConstructor
public class NotificationModule {
    
    public void init() {
        log.info("Notification module initialized");
    }
}
