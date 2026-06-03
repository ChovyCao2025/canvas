package org.chovy.canvas.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigGuardTest {

    @Test
    void rejectsWildcardCorsWhenCredentialsAreAllowed() {
        ProductionConfigGuard guard = new ProductionConfigGuard(
                List.of("*"),
                true,
                "strong-secret-strong-secret-1234",
                "jwt-secret-jwt-secret-jwt-secret-1234",
                "canvas_app",
                "not-root");

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CORS wildcard");
    }

    @Test
    void rejectsDefaultEventReportSecret() {
        ProductionConfigGuard guard = new ProductionConfigGuard(
                List.of("https://app.photonpay.com"),
                true,
                "canvas-event-report-secret-2026!!",
                "jwt-secret-jwt-secret-jwt-secret-1234",
                "canvas_app",
                "not-root");

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("event report secret");
    }

    @Test
    void rejectsBlankJwtSecret() {
        ProductionConfigGuard guard = new ProductionConfigGuard(
                List.of("https://app.photonpay.com"),
                true,
                "strong-secret-strong-secret-1234",
                " ",
                "canvas_app",
                "not-root");

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt secret");
    }

    @Test
    void acceptsStrongProductionSettings() {
        ProductionConfigGuard guard = new ProductionConfigGuard(
                List.of("https://app.photonpay.com"),
                true,
                "strong-secret-strong-secret-1234",
                "jwt-secret-jwt-secret-jwt-secret-1234",
                "canvas_app",
                "not-root");

        assertThatCode(guard::validate).doesNotThrowAnyException();
    }
}
