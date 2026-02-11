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

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // DB 사용 시
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")

    // 보안 필요 시(로그인/회원가입)
    implementation("org.springframework.boot:spring-boot-starter-security")


    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // AWS S3
    implementation("software.amazon.awssdk:s3:2.41.23");

    testImplementation("org.springframework.boot:spring-boot-starter-test")


}
