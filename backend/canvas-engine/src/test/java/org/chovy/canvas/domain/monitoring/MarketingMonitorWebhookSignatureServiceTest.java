package org.chovy.canvas.domain.monitoring;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketingMonitorWebhookSignatureServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-06T00:00:00Z"),
            ZoneOffset.UTC);

    @Test
    void signsAndVerifiesRawBodyWithTimestamp() {
        MarketingMonitorWebhookSignatureService service =
                new MarketingMonitorWebhookSignatureService(CLOCK);
        String timestamp = String.valueOf(CLOCK.instant().getEpochSecond());
        String rawBody = "{\"id\":\"mention-1\",\"text\":\"bad support\"}";

        String signature = service.sign("whsec_test", timestamp, rawBody);

        assertThat(signature).startsWith("sha256=");
        assertThatCode(() -> service.verifyOrThrow(
                "whsec_test",
                timestamp,
                rawBody,
                signature,
                300))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsStaleTimestampAndInvalidSignature() {
        MarketingMonitorWebhookSignatureService service =
                new MarketingMonitorWebhookSignatureService(CLOCK);
        String timestamp = String.valueOf(CLOCK.instant().minusSeconds(301).getEpochSecond());
        String rawBody = "{\"id\":\"mention-1\",\"text\":\"bad support\"}";
        String signature = service.sign("whsec_test", timestamp, rawBody);

        assertThatThrownBy(() -> service.verifyOrThrow("whsec_test", timestamp, rawBody, signature, 300))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        String currentTimestamp = String.valueOf(CLOCK.instant().getEpochSecond());
        assertThatThrownBy(() -> service.verifyOrThrow(
                "whsec_test",
                currentTimestamp,
                rawBody,
                "sha256=invalid",
                300))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
