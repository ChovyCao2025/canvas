package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.chovy.canvas.canvas.api.ContactabilityFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class ContactabilityControllerCompatibilityTest {

    @Test
    void explainRoutePreservesLegacyEnvelopeAndDelegatesAllRequestFields() {
        RecordingContactabilityFacade facade = new RecordingContactabilityFacade();

        webClient(facade)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/contactability/explain")
                        .queryParam("userId", "user-1")
                        .queryParam("channel", "sms")
                        .queryParam("requireExplicitConsent", false)
                        .queryParam("quietStart", "21:30")
                        .queryParam("quietEnd", "07:15")
                        .queryParam("quietTimezone", "Asia/Shanghai")
                        .queryParam("canvasId", 12)
                        .queryParam("nodeId", "message-1")
                        .queryParam("frequencyScope", "CHANNEL")
                        .queryParam("frequencyMax", 3)
                        .queryParam("frequencyWindowSeconds", 3600)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.userId").isEqualTo("user-1")
                .jsonPath("$.data.channel").isEqualTo("SMS")
                .jsonPath("$.data.allowed").isEqualTo(true)
                .jsonPath("$.data.checks[0].checkKey").isEqualTo("CONSENT")
                .jsonPath("$.data.checks[0].allowed").isEqualTo(true)
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        assertThat(facade.lastRequest).isNotNull();
        assertThat(facade.lastRequest.userId()).isEqualTo("user-1");
        assertThat(facade.lastRequest.channel()).isEqualTo("sms");
        assertThat(facade.lastRequest.requireExplicitConsent()).isFalse();
        assertThat(facade.lastRequest.quietStart()).isEqualTo("21:30");
        assertThat(facade.lastRequest.quietEnd()).isEqualTo("07:15");
        assertThat(facade.lastRequest.quietTimezone()).isEqualTo("Asia/Shanghai");
        assertThat(facade.lastRequest.canvasId()).isEqualTo(12L);
        assertThat(facade.lastRequest.nodeId()).isEqualTo("message-1");
        assertThat(facade.lastRequest.frequencyScope()).isEqualTo("CHANNEL");
        assertThat(facade.lastRequest.frequencyMax()).isEqualTo(3);
        assertThat(facade.lastRequest.frequencyWindow()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void explainRouteUsesLegacyDefaultsAndSafetyFallbacksForOptionalFields() {
        RecordingContactabilityFacade facade = new RecordingContactabilityFacade();

        webClient(facade)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/canvas/contactability/explain")
                        .queryParam("userId", "user-2")
                        .queryParam("channel", "email")
                        .queryParam("quietStart", "not-time")
                        .queryParam("quietEnd", "25:99")
                        .queryParam("quietTimezone", "Mars/Base")
                        .queryParam("nodeId", " ")
                        .queryParam("frequencyMax", -1)
                        .queryParam("frequencyWindowSeconds", -10)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.channel").isEqualTo("EMAIL");

        assertThat(facade.lastRequest).isNotNull();
        assertThat(facade.lastRequest.userId()).isEqualTo("user-2");
        assertThat(facade.lastRequest.channel()).isEqualTo("email");
        assertThat(facade.lastRequest.requireExplicitConsent()).isTrue();
        assertThat(facade.lastRequest.quietStart()).isEqualTo("22:00");
        assertThat(facade.lastRequest.quietEnd()).isEqualTo("08:00");
        assertThat(facade.lastRequest.quietTimezone()).isEqualTo("USER_LOCAL");
        assertThat(facade.lastRequest.canvasId()).isEqualTo(0L);
        assertThat(facade.lastRequest.nodeId()).isEqualTo("preflight");
        assertThat(facade.lastRequest.frequencyScope()).isEqualTo("JOURNEY");
        assertThat(facade.lastRequest.frequencyMax()).isEqualTo(1);
        assertThat(facade.lastRequest.frequencyWindow()).isEqualTo(Duration.ofDays(1));
    }

    private static WebTestClient webClient(ContactabilityFacade facade) {
        return WebTestClient.bindToController(new ContactabilityController(facade)).build();
    }

    private static final class RecordingContactabilityFacade implements ContactabilityFacade {
        private Request lastRequest;

        @Override
        public Report explain(Request request) {
            lastRequest = request;
            String channel = request.channel() == null || request.channel().isBlank()
                    ? "ALL"
                    : request.channel().toUpperCase();
            return new Report(
                    request.userId(),
                    channel,
                    true,
                    List.of(new Check("CONSENT", true, null, null)));
        }
    }
}
