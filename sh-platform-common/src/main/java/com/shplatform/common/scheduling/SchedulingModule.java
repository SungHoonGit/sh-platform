package com.shplatform.common.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ComponentScan(basePackages = "com.shplatform.common.scheduling")
@RequiredArgsConstructor
public class SchedulingModule {
    
    public void init() {
        log.info("Scheduling module initialized");
    }
}
