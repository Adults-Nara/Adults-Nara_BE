package com.ott.media;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.ott.common.persistence.entity")
@EnableJpaRepositories(basePackages = "com.ott.media")
public class MediaWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MediaWorkerApplication.class, args);
    }
}
