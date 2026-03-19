package com.ott.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableRetry
@EnableScheduling
@EnableAsync
@SpringBootApplication(scanBasePackages = {"com.ott.core", "com.ott.common.outbox"})
@EntityScan(basePackages = {"com.ott.common.persistence.entity", "com.ott.common.outbox.entity"})
@EnableJpaRepositories(basePackages = {"com.ott.core", "com.ott.common.outbox.repository"})
public class CoreApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreApiApplication.class, args);
    }
}
