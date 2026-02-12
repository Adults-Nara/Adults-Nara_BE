plugins {
    id("java-library")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

dependencies {
    // 엔티티 컴파일에 필요한 최소 의존성만
    api("jakarta.persistence:jakarta.persistence-api:3.1.0")

    implementation("io.hypersistence:hypersistence-utils-hibernate-60:3.5.1") // TSID
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    api ("org.projectlombok:lombok:1.18.34")
    annotationProcessor ("org.projectlombok:lombok:1.18.34")

    testCompileOnly ("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.34")

}
