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
                "not-root",
                "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=",
                "asset-webhook-secret-asset-webhook-1234",
                false,
                "",
                "",
                "",
                "",
                "",
                false,
                0L);

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
                "not-root",
                "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=",
                "asset-webhook-secret-asset-webhook-1234",
                false,
                "",
                "",
                "",
                "",
                "",
                false,
                0L);

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
                "not-root",
                "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=",
                "asset-webhook-secret-asset-webhook-1234",
                false,
                "",
                "",
                "",
                "",
                "",
                false,
                0L);

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
                "not-root",
                "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=",
                "asset-webhook-secret-asset-webhook-1234",
                false,
                "",
                "",
                "",
                "",
                "",
                false,
                0L);

        assertThatCode(guard::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsDefaultSecretCipherKey() {
        ProductionConfigGuard guard = new ProductionConfigGuard(
                List.of("https://app.photonpay.com"),
                true,
                "strong-secret-strong-secret-1234",
                "jwt-secret-jwt-secret-jwt-secret-1234",
                "canvas_app",
                "not-root",
                "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "asset-webhook-secret-asset-webhook-1234",
                false,
                "",
                "",
                "",
                "",
                "",
                false,
                0L);

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("secret cipher key");
    }

    @Test
    void rejectsBlankAssetUploadWebhookSecret() {
        ProductionConfigGuard guard = new ProductionConfigGuard(
                List.of("https://app.photonpay.com"),
                true,
                "strong-secret-strong-secret-1234",
                "jwt-secret-jwt-secret-jwt-secret-1234",
                "canvas_app",
                "not-root",
                "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=",
                " ",
                false,
                "",
                "",
                "",
                "",
                "",
                false,
                0L);

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("asset upload webhook secret");
    }

    @Test
    void rejectsAssetUploadWebhookToleranceAboveReplayWindow() {
        ProductionConfigGuard guard = new ProductionConfigGuard(
                List.of("https://app.photonpay.com"),
                true,
                "strong-secret-strong-secret-1234",
                "jwt-secret-jwt-secret-jwt-secret-1234",
                "canvas_app",
                "not-root",
                "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=",
                "asset-webhook-secret-asset-webhook-1234",
                false,
                "",
                "",
                "",
                "",
                "",
                false,
                0L,
                301);

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("asset upload webhook tolerance");
    }

    @Test
    void rejectsEnabledAssetUploadS3WithoutHttpsEndpointAndPublicBaseUrl() {
        ProductionConfigGuard guard = new ProductionConfigGuard(
                List.of("https://app.photonpay.com"),
                true,
                "strong-secret-strong-secret-1234",
                "jwt-secret-jwt-secret-jwt-secret-1234",
                "canvas_app",
                "not-root",
                "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=",
                "asset-webhook-secret-asset-webhook-1234",
                true,
                "http://minio.internal:9000",
                "canvas-assets",
                "asset-access-key",
                "asset-secret-key-1234",
                "https://cdn.example.com/assets",
                false,
                0L);

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("S3 endpoint");
    }

    @Test
    void acceptsEnabledAssetUploadS3WithHttpsStorageSettings() {
        ProductionConfigGuard guard = new ProductionConfigGuard(
                List.of("https://app.photonpay.com"),
                true,
                "strong-secret-strong-secret-1234",
                "jwt-secret-jwt-secret-jwt-secret-1234",
                "canvas_app",
                "not-root",
                "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=",
                "asset-webhook-secret-asset-webhook-1234",
                true,
                "https://s3.example.com",
                "canvas-assets",
                "asset-access-key",
                "asset-secret-key-1234",
                "https://cdn.example.com/assets",
                false,
                0L);

        assertThatCode(guard::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsEnabledAssetUploadCleanupWithoutTenantId() {
        ProductionConfigGuard guard = new ProductionConfigGuard(
                List.of("https://app.photonpay.com"),
                true,
                "strong-secret-strong-secret-1234",
                "jwt-secret-jwt-secret-jwt-secret-1234",
                "canvas_app",
                "not-root",
                "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=",
                "asset-webhook-secret-asset-webhook-1234",
                false,
                "",
                "",
                "",
                "",
                "",
                true,
                0L);

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("asset upload cleanup tenant id");
    }
}
