package com.scraper.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${SCRAPER_BASE_URL:https://sunghoonyk.duckdns.org/scraper}")
    private String baseUrl;

    @Value("${SCRAPER_PORT:8081}")
    private int port;

    @Bean
    public OpenAPI openAPI() {
        List<Server> servers = new ArrayList<>();
        servers.add(new Server().url(baseUrl).description("Production"));
        if (port != 80) {
            servers.add(new Server().url("http://localhost:" + port).description("Local"));
        }
        return new OpenAPI()
                .info(new Info()
                        .title("Scraper Platform API")
                        .description("스크래핑 데이터 관리 및 모니터링 API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SungHoon")
                                .email("sung@sunghoonyk.duckdns.org")))
                .servers(servers);
    }
}
