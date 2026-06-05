package org.chovy.canvas.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionProfileValidationTest {

    @Test
    void productionLikeProfilesUseEnvBackedSafeConfiguration() throws Exception {
        for (String profile : List.of("application-prod.yml", "application-staging.yml")) {
            String yaml = Files.readString(resourcePath(profile));

            assertThat(yaml).contains("${SPRING_DATASOURCE_URL");
            assertThat(yaml).contains("${SPRING_DATASOURCE_USERNAME");
            assertThat(yaml).contains("${SPRING_DATASOURCE_PASSWORD");
            assertThat(yaml).contains("${SPRING_DATA_REDIS_PASSWORD");
            assertThat(yaml).contains("${CANVAS_CORS_ALLOWED_ORIGINS");
            assertThat(yaml).contains("${CANVAS_EVENT_REPORT_SECRET");
            assertThat(yaml).contains("${CANVAS_JWT_SECRET");
            assertThat(yaml).contains("${CANVAS_SECRET_CIPHER_KEY");
            assertThat(yaml).contains("include: health,info,prometheus,metrics");
            assertThat(yaml).contains("show-details: when-authorized");
            assertThat(yaml).contains("enabled: false");

            assertThat(yaml).doesNotContain("username: root");
            assertThat(yaml).doesNotContain("password: root");
            assertThat(yaml).doesNotContain("allowed-origins: \"*\"");
            assertThat(yaml).doesNotContain("allowed-origins: '*'");
            assertThat(yaml).doesNotContain("allowed-origins: *");
            assertThat(yaml).doesNotContain("canvas-event-report-secret-2026!!");
            assertThat(yaml).doesNotContain("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
            assertThat(yaml).doesNotContain("show-details: always");
        }
    }

    private Path resourcePath(String fileName) {
        Path modulePath = Path.of("src/main/resources/" + fileName);
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        return Path.of("canvas-engine/src/main/resources/" + fileName);
    }
}
