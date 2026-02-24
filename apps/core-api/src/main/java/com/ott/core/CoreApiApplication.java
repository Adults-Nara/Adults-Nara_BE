package com.ott.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
@EntityScan(basePackages = "com.ott.common.persistence.entity")
@EnableJpaRepositories(basePackages = "com.ott.core")
public class CoreApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreApiApplication.class, args);
    }
}
