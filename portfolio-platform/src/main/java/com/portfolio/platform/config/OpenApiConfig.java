package com.portfolio.platform.config;

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

    @Value("${PORTFOLIO_BASE_URL:https://sunghoonyk.duckdns.org/portfolio}")
    private String baseUrl;

    @Value("${PORTFOLIO_PORT:8083}")
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
                        .title("Portfolio Platform API")
                        .description("포트폴리오 관리 API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SungHoon")
                                .email("sung@sunghoonyk.duckdns.org")))
                .servers(servers);
    }
}
