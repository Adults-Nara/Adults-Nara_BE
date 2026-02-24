package com.ott.core.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
public class SwaggerConfig {
    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    @Profile("prod")
    public OpenAPI openApiProd() {
        return new OpenAPI()
                .servers(List.of(new Server().url("https://api.asinna.store")));
    }

    @Bean
    @Profile({"local", "dev"})
    public OpenAPI openApiLocal() {
        return new OpenAPI()
                .servers(List.of(new Server().url("http://localhost:8080")));
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                // 1) Security Scheme 정의
                .components(new Components().addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                // 2) 전역 Security Requirement (모든 API에 기본 적용)
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
