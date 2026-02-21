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
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    // Spring Batch
    implementation("org.springframework.boot:spring-boot-starter-batch")

    testImplementation("org.springframework.boot:spring-boot-starter-test")


    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
}

