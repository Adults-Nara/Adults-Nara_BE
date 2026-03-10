plugins {
    id("org.springframework.boot") version "3.4.2" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("jacoco")
    id("org.sonarqube") version "5.0.0.4638"
}

allprojects {
    group = "com.ott"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "jacoco")

    tasks.withType<JacocoReport> {
        dependsOn(tasks.withType<Test>())
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/Q*.class",
                        "**/*Entity*.class",
                        "**/dto/**",
                        "**/config/**",
                        "**/exception/**"
                    )
                }
            })
        )
    }

    tasks.withType<JacocoCoverageVerification> {
        dependsOn(tasks.withType<JacocoReport>())
        violationRules {
            rule {
                element = "CLASS"
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.70".toBigDecimal()
                }
                excludes = listOf(
                    "**.Q*",
                    "**.dto.**",
                    "**.config.**",
                    "**.exception.**",
                    "**.*Entity*"
                )
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy(tasks.withType<JacocoReport>())
    }

    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(tasks.withType<JacocoCoverageVerification>())
    }
}

sonar {
    properties {
        property("sonar.organization", "adults-nara")
        property("sonar.projectKey", "Adults-Nara_Adults-Nara_BE")
        property("sonar.projectName", "Adults-Nara_BE")
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.coverage.jacoco.xmlReportPaths", "${project.rootDir}/**/build/reports/jacoco/test/jacocoTestReport.xml")
    }
}
