package com.ott.core.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;

@Configuration
public class SwaggerConfig {
    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Bean
    public OpenAPI openAPI() {
        OpenAPI openApi = new OpenAPI()
                // 1) Security Scheme 정의
                .components(new Components().addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // 2) 전역 Security Requirement
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));

        // 3) Profile에 따른 Server URL 설정 (ALB 뒤에 있을 때 HTTP/HTTPS Mixed Content 및 CORS 방지)
        if ("prod".equals(activeProfile)) {
            openApi.servers(List.of(new Server().url("https://api.asinna.store")));
        } else {
            openApi.servers(List.of(new Server().url("http://localhost:8080")));
        }

        return openApi;
    }
}
