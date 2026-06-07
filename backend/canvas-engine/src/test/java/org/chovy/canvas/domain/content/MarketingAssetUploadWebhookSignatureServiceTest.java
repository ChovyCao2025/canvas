package org.chovy.canvas.domain.content;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketingAssetUploadWebhookSignatureServiceTest {

    @Test
    void verifiesFreshHmacSignature() {
        MarketingAssetUploadWebhookSignatureService service = service();
        String body = "{\"uploadToken\":\"token-1\",\"status\":\"READY\"}";
        String signature = service.sign("1780704000", body);

        assertThatCode(() -> service.verifyOrThrow("1780704000", body, signature))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsStaleOrInvalidSignature() {
        MarketingAssetUploadWebhookSignatureService service = service();
        String body = "{\"uploadToken\":\"token-1\",\"status\":\"READY\"}";

        assertThatThrownBy(() -> service.verifyOrThrow("1780700000", body, service.sign("1780700000", body)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("stale asset upload webhook timestamp");

        assertThatThrownBy(() -> service.verifyOrThrow("1780704000", body, "sha256=bad"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalid asset upload webhook signature");
    }

    private MarketingAssetUploadWebhookSignatureService service() {
        return new MarketingAssetUploadWebhookSignatureService(
                "asset-webhook-secret-asset-webhook-1234",
                300,
                Clock.fixed(Instant.ofEpochSecond(1780704000), ZoneOffset.UTC));
    }
}
