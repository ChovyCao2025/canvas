plugins {
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.5"
    java
}

group = "com.photon"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // WebFlux（Netty）
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    // Spring Security WebFlux
    implementation("org.springframework.boot:spring-boot-starter-security")

    // MyBatis-Plus（blocking DB，配合 boundedElastic 使用）
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.7")
    implementation("com.mysql:mysql-connector-j")

    // Redis（reactive Lettuce）
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // Caffeine L1 本地缓存
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Flyway DB migration
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // BCrypt
    implementation("org.springframework.security:spring-security-crypto")

    // Groovy（沙箱脚本执行）
    implementation("org.apache.groovy:groovy:4.0.21")

    // Actuator + Micrometer + Prometheus
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Swagger / OpenAPI（生产禁用）
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.5.0")

    // 结构化 JSON 日志（logback-spring.xml 使用 LogstashEncoder）
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
