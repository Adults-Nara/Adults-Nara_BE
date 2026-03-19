plugins {
    id("java-library")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

dependencies {
    // Spring (for Lombok NullAnnotations)
    compileOnly("org.springframework.boot:spring-boot-starter:3.4.2")

    // 엔티티 컴파일에 필요한 최소 의존성만
    api("jakarta.persistence:jakarta.persistence-api:3.1.0")

    // Querydsl
    api("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api:3.1.0")

    // pgvector support
    api("org.hibernate.orm:hibernate-vector:6.6.4.Final")

    implementation("io.hypersistence:hypersistence-utils-hibernate-60:3.5.1") // TSID
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // Outbox 패턴: OutboxPublisher, OutboxEventRepository 컴파일에 필요
    compileOnly("org.springframework.data:spring-data-jpa:3.4.2")
    compileOnly("org.springframework:spring-tx:6.2.2")
    compileOnly("org.springframework:spring-context:6.2.2")
    compileOnly("org.springframework.kafka:spring-kafka:3.3.2")

    api ("org.projectlombok:lombok:1.18.34")
    annotationProcessor ("org.projectlombok:lombok:1.18.34")

    testCompileOnly ("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.34")

}
