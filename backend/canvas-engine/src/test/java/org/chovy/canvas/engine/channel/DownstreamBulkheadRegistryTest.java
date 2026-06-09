package org.chovy.canvas.engine.channel;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DownstreamBulkheadRegistryTest {

    @Test
    void opensProviderSpecificBulkhead() {
        DownstreamBulkheadRegistry registry = DownstreamBulkheadRegistry.available();
        Instant now = Instant.parse("2026-06-09T09:00:00Z");

        registry.open(7L, "whatsapp", "CHANNEL", Duration.ofSeconds(30), "timeout", now);

        assertThat(registry.permit(7L, "whatsapp", "CHANNEL", now).permitted()).isFalse();
        assertThat(registry.permit(7L, "email", "CHANNEL", now).permitted()).isTrue();
    }

    @Test
    void allowsHalfOpenRecovery() {
        DownstreamBulkheadRegistry registry = DownstreamBulkheadRegistry.available();
        Instant now = Instant.parse("2026-06-09T09:00:00Z");
        registry.open(7L, "sms", "CHANNEL", Duration.ofSeconds(30), "timeout", now);

        DownstreamBulkheadRegistry.Decision decision =
                registry.permit(7L, "sms", "CHANNEL", now.plusSeconds(31));

        assertThat(decision.permitted()).isTrue();
        assertThat(decision.state()).isEqualTo(DownstreamBulkheadRegistry.State.HALF_OPEN);

        registry.close(7L, "sms", "CHANNEL");
        assertThat(registry.permit(7L, "sms", "CHANNEL", now.plusSeconds(32)).state())
                .isEqualTo(DownstreamBulkheadRegistry.State.CLOSED);
    }

    @Test
    void failsClosedWhenRegistryStateIsUnavailable() {
        DownstreamBulkheadRegistry registry = DownstreamBulkheadRegistry.unavailable();

        DownstreamBulkheadRegistry.Decision decision =
                registry.permit(7L, "push", "CHANNEL", Instant.parse("2026-06-09T09:00:00Z"));

        assertThat(decision.permitted()).isFalse();
        assertThat(decision.reason()).contains("unavailable");
        assertThat(decision.retryAfter()).isNotNull();
    }
}
