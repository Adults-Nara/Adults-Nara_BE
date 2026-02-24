package com.ott.core.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
public class SwaggerConfig {
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
}
