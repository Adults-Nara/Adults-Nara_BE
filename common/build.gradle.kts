plugins {
    id("java-library")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

dependencies {
    // 엔티티 컴파일에 필요한 최소 의존성만
    api("jakarta.persistence:jakarta.persistence-api:3.1.0")

    // 선택: Auditing 쓰면 필요
    api("org.springframework.data:spring-data-jpa")
}
