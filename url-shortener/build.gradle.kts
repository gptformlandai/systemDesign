import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    id("org.springframework.boot") version "3.3.2"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.urlshortener"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

val resilience4jVersion = "2.2.0"
val jjwtVersion = "0.12.6"
val archunitVersion = "1.3.0"
val testcontainersVersion = "1.20.1"
val logstashVersion = "7.4"

dependencies {
    // ── Core WebFlux ──────────────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.6.0")

    // ── Reactive R2DBC + PostgreSQL ───────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.postgresql:r2dbc-postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    // Flyway requires a JDBC driver (blocking, startup only)
    runtimeOnly("org.postgresql:postgresql")

    // ── Reactive Redis ────────────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // ── Reactor Kafka ─────────────────────────────────────────────────────────
    implementation("io.projectreactor.kafka:reactor-kafka")

    // ── Spring Security + JWT ─────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:${jjwtVersion}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${jjwtVersion}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${jjwtVersion}")

    // ── Resilience4j (circuit breaker) ────────────────────────────────────────
    implementation("io.github.resilience4j:resilience4j-spring-boot3:${resilience4jVersion}")
    implementation("io.github.resilience4j:resilience4j-reactor:${resilience4jVersion}")

    // ── Observability ─────────────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("net.logstash.logback:logstash-logback-encoder:${logstashVersion}")

    // ── Local L1 Cache (Caffeine) ─────────────────────────────────────────────
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // ── Lombok ────────────────────────────────────────────────────────────────
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.security:spring-security-test")

    // Testcontainers
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("com.redis:testcontainers-redis:2.2.2")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:r2dbc")

    // ArchUnit
    testImplementation("com.tngtech.archunit:archunit-junit5:${archunitVersion}")

    // Mockito (WebFlux compatible)
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:${testcontainersVersion}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

// Separate integration test source set
sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}

configurations["integrationTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["integrationTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

val integrationTest = task<Test>("integrationTest") {
    description = "Runs integration tests (Testcontainers)"
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter("test")
}

tasks.getByName<BootJar>("bootJar") {
    archiveClassifier.set("boot")
}
