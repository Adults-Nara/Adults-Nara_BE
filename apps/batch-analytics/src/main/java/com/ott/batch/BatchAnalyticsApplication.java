package com.ott.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableBatchProcessing
@EnableScheduling
@SpringBootApplication
@EntityScan(basePackages = "com.ott.common.persistence.entity")
@EnableJpaRepositories(basePackages = "com.ott.batch")
public class BatchAnalyticsApplication {
    public static void main(String[] args) {
        SpringApplication.run(BatchAnalyticsApplication.class, args);
    }
}
