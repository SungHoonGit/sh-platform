package com.scraper.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Scraper Platform API")
                        .description("스크래핑 데이터 관리 및 모니터링 API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SungHoon")
                                .email("sung@sunghoonyk.duckdns.org")))
                .servers(List.of(
                        new Server().url("https://sunghoonyk.duckdns.org/scraper").description("Production"),
                        new Server().url("http://localhost:8081/api").description("Local")));
    }
}
