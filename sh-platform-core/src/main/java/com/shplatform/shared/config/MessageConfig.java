package com.shplatform.shared.config;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
public class MessageConfig {

    @Bean
    public MessageSource messageSource() {
        var ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:messages/messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setCacheSeconds(3600);
        return ms;
    }

    @Bean
    public AcceptHeaderLocaleResolver localeResolver() {
        var resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.KOREA);
        return resolver;
    }
}
