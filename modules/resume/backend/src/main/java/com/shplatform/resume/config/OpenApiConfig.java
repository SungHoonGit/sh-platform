package com.shplatform.resume.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Value("${RESUME_BASE_URL:https://sunghoonyk.duckdns.org/resume}")
    private String baseUrl;

    @Value("${RESUME_PORT:8082}")
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
                        .title("Resume Platform API")
                        .description("이력서 관리 API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SungHoon")
                                .email("sung@sunghoonyk.duckdns.org")))
                .servers(servers)
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
