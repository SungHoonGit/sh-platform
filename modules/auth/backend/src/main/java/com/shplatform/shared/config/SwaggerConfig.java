package com.shplatform.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${swagger.server-url:http://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("sh-platform API")
                        .description("인증 · AI Housing 통합 플랫폼 API 문서<br>"
                                + "<small><a href='/test-reports/' target='_blank'>테스트 리포트</a>"
                                + " | <a href='/javadoc/' target='_blank'>Javadoc</a></small>")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SungHoon")
                                .email("ksa134652@gmail.com")))
                .servers(List.of(
                        new Server().url(serverUrl).description("현재 서버"),
                        new Server().url("http://localhost:8080").description("로컬 개발")
                ));
    }
}
