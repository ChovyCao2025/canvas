package org.chovy.canvas.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionSecurityValidatorTest {

    @Test
    void productionProfileRejectsLocalDefaultsAndExposedHealthDetails() {
        MockEnvironment env = productionEnvironment()
                .withProperty("spring.datasource.username", "root")
                .withProperty("spring.datasource.password", "root")
                .withProperty("canvas.datasource.credential-secret", "canvas-local-datasource-secret-32b!")
                .withProperty("canvas.jwt.secret", "short")
                .withProperty("canvas.events.report-secret", "canvas-event-report-secret-2026!!")
                .withProperty("canvas.cors.allowed-origins", "*")
                .withProperty("management.endpoint.health.show-details", "always")
                .withProperty("springdoc.api-docs.enabled", "true")
                .withProperty("springdoc.swagger-ui.enabled", "true");

        assertThatThrownBy(() -> new ProductionSecurityValidator(env).afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.datasource.username")
                .hasMessageContaining("canvas.datasource.credential-secret")
                .hasMessageContaining("canvas.jwt.secret")
                .hasMessageContaining("canvas.events.report-secret")
                .hasMessageContaining("canvas.cors.allowed-origins")
                .hasMessageContaining("management.endpoint.health.show-details")
                .hasMessageContaining("springdoc.api-docs.enabled")
                .hasMessageContaining("springdoc.swagger-ui.enabled");
    }

    @Test
    void localProfileKeepsDeveloperDefaultsAllowed() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.profiles.active", "local")
                .withProperty("spring.datasource.username", "root")
                .withProperty("spring.datasource.password", "root")
                .withProperty("canvas.events.report-secret", "canvas-event-report-secret-2026!!")
                .withProperty("canvas.cors.allowed-origins", "*")
                .withProperty("management.endpoint.health.show-details", "always");

        assertThatCode(() -> new ProductionSecurityValidator(env).afterSingletonsInstantiated())
                .doesNotThrowAnyException();
    }

    private MockEnvironment productionEnvironment() {
        return new MockEnvironment().withProperty("spring.profiles.active", "prod");
    }
}
