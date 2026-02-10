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

    compileOnly ("org.projectlombok:lombok:1.18.34")
    annotationProcessor ("org.projectlombok:lombok:1.18.34")

    testCompileOnly ("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.34")

    // 보안 필요 시(로그인/회원가입)
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("org.flywaydb:flyway-core:10.22.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.22.0")

    // AWS S3
    implementation("software.amazon.awssdk:s3:2.41.23");

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
