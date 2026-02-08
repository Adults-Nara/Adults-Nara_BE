plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("java")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter")

    // 메시지 큐를 Kafka로 갈 거면
    //implementation("org.springframework.kafka:spring-kafka")

    // S3 접근(초기엔 AWS SDK v2 권장)
    implementation("software.amazon.awssdk:s3:2.25.62")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
