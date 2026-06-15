package org.chovy.canvas.web.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.marketing.api.MarketingPreferenceFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class MarketingPreferenceControllerCompatibilityTest {

    @Test
    void exposesFiveLegacyPreferenceRoutesWithEnvelopeAndForwarding() {
        RecordingMarketingPreferenceFacade facade = new RecordingMarketingPreferenceFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/canvas/marketing-preferences/users/user-1")
                .header("X-Tenant-Id", "7")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.userId").isEqualTo("user-1")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        client.put()
                .uri("/canvas/marketing-preferences/users/user-1/consents/sms")
                .header("X-Tenant-Id", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"consentStatus":"OPT_IN","source":"form"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.channel").isEqualTo("SMS")
                .jsonPath("$.data.consentStatus").isEqualTo("OPT_IN");

        client.put()
                .uri("/canvas/marketing-preferences/users/user-1/channels/sms")
                .header("X-Tenant-Id", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"address":"+15550001","enabled":true,"verified":false,"metadata":"{}"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.address").isEqualTo("+15550001")
                .jsonPath("$.data.reachable").isEqualTo(true);

        client.post()
                .uri("/canvas/marketing-preferences/users/user-1/suppressions")
                .header("X-Tenant-Id", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"channel":"email","reason":"complaint","active":true,"expiresAt":"2026-06-20T12:00:00"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.channel").isEqualTo("EMAIL")
                .jsonPath("$.data.state").isEqualTo("ACTIVE");

        client.put()
                .uri("/canvas/marketing-preferences/suppressions/99/deactivate")
                .header("X-Tenant-Id", "7")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").doesNotExist();

        assertThat(facade.operations).containsExactly("report", "updateConsent", "updateChannel",
                "addSuppression", "deactivateSuppression");
        assertThat(facade.lastTenantId).isEqualTo(7L);
        assertThat(facade.lastUserId).isEqualTo("user-1");
        assertThat(facade.lastChannel).isEqualTo("email");
        assertThat(facade.lastSuppressionId).isEqualTo(99L);
    }

    @Test
    void missingTenantHeaderDefaultsToLegacyZeroTenant() {
        RecordingMarketingPreferenceFacade facade = new RecordingMarketingPreferenceFacade();

        webClient(facade)
                .get()
                .uri("/canvas/marketing-preferences/users/user-2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.userId").isEqualTo("user-2");

        assertThat(facade.lastTenantId).isEqualTo(0L);
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingMarketingPreferenceFacade facade = new RecordingMarketingPreferenceFacade();
        facade.failConsent = true;

        webClient(facade)
                .put()
                .uri("/canvas/marketing-preferences/users/user-1/consents/sms")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"consentStatus":"MAYBE"}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Unsupported consent status: MAYBE")
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(MarketingPreferenceFacade facade) {
        return WebTestClient.bindToController(new MarketingPreferenceController(facade)).build();
    }

    private static final class RecordingMarketingPreferenceFacade implements MarketingPreferenceFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastUserId;
        private String lastChannel;
        private Long lastSuppressionId;
        private boolean failConsent;

        @Override
        public PreferenceReport report(Long tenantId, String userId) {
            operations.add("report");
            lastTenantId = tenantId;
            lastUserId = userId;
            return new PreferenceReport(userId, List.of(), List.of(), List.of(),
                    new PreferenceSummary(0, 0, 0, 0, 0));
        }

        @Override
        public ConsentRow updateConsent(Long tenantId, String userId, ConsentUpdateCommand command) {
            operations.add("updateConsent");
            lastTenantId = tenantId;
            lastUserId = userId;
            lastChannel = command.channel();
            if (failConsent) {
                throw new IllegalArgumentException("Unsupported consent status: MAYBE");
            }
            return new ConsentRow(command.channel().toUpperCase(), command.consentStatus(), command.source(),
                    LocalDateTime.parse("2026-06-15T01:50:00"));
        }

        @Override
        public ChannelRow updateChannel(Long tenantId, String userId, ChannelUpdateCommand command) {
            operations.add("updateChannel");
            lastTenantId = tenantId;
            lastUserId = userId;
            lastChannel = command.channel();
            return new ChannelRow(command.channel().toUpperCase(), command.address(), command.enabled(),
                    command.verified(), true, command.metadata(), LocalDateTime.parse("2026-06-15T01:50:00"));
        }

        @Override
        public SuppressionRow addSuppression(Long tenantId, String userId, SuppressionCreateCommand command) {
            operations.add("addSuppression");
            lastTenantId = tenantId;
            lastUserId = userId;
            lastChannel = command.channel();
            return new SuppressionRow(99L, command.channel().toUpperCase(), command.reason(), true, "ACTIVE",
                    command.expiresAt(), LocalDateTime.parse("2026-06-15T01:50:00"),
                    LocalDateTime.parse("2026-06-15T01:50:00"));
        }

        @Override
        public void deactivateSuppression(Long tenantId, Long suppressionId) {
            operations.add("deactivateSuppression");
            lastTenantId = tenantId;
            lastSuppressionId = suppressionId;
        }
    }
}
