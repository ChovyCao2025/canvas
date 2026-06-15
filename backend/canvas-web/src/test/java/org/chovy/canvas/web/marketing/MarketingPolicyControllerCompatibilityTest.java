package org.chovy.canvas.web.marketing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MarketingPolicyFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class MarketingPolicyControllerCompatibilityTest {

    @Test
    void mapsLegacyPolicyRoutesToFacadeWithCompatibilityEnvelope() {
        RecordingMarketingPolicyFacade facade = new RecordingMarketingPolicyFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                get("/state?userId=user-1&channel=email", "policyState"),
                post("/consent", "upsertConsent", Map.of("userId", "user-1", "channel", "email",
                        "consentStatus", "granted")),
                post("/suppression", "upsertSuppression", Map.of("userId", "user-1", "reason", "complaint")),
                post("/channel", "upsertChannel", Map.of("userId", "user-1", "channel", "email",
                        "address", "buyer@example.com")));

        for (RouteProbe probe : probes) {
            probe.exchange(client)
                    .expectStatus().isOk()
                    .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(0)
                    .jsonPath("$.message").isEqualTo("success")
                    .jsonPath("$.errorCode").doesNotExist()
                    .jsonPath("$.traceId").doesNotExist();
        }

        assertThat(facade.operations).containsExactlyElementsOf(probes.stream()
                .map(RouteProbe::operation)
                .toList());
    }

    @Test
    void forwardsTenantHeaderQueriesAndBodies() {
        RecordingMarketingPolicyFacade facade = new RecordingMarketingPolicyFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/policies/consent")
                .header("X-Tenant-Id", "42")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "userId": "user-1",
                          "channel": "email",
                          "consentStatus": "granted",
                          "source": "web"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(42)
                .jsonPath("$.data.channel").isEqualTo("email")
                .jsonPath("$.data.source").isEqualTo("web");

        assertThat(facade.lastTenantId).isEqualTo(42L);
        assertThat(facade.lastConsentCommand.consentStatus()).isEqualTo("granted");

        client.get()
                .uri("/canvas/policies/state?userId=user-1&channel=email")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastTenantId).isEqualTo(0L);
        assertThat(facade.lastUserId).isEqualTo("user-1");
        assertThat(facade.lastChannel).isEqualTo("email");
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingMarketingPolicyFacade facade = new RecordingMarketingPolicyFacade();
        facade.failConsent = true;

        webClient(facade)
                .post()
                .uri("/canvas/policies/consent")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"userId": " "}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("userId is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(MarketingPolicyFacade facade) {
        return WebTestClient.bindToController(new MarketingPolicyController(facade)).build();
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe("GET", path, operation, Map.of());
    }

    private static RouteProbe post(String path, String operation, Map<String, Object> payload) {
        return new RouteProbe("POST", path, operation, payload);
    }

    private record RouteProbe(String method, String path, String operation, Map<String, Object> payload) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            String uri = "/canvas/policies" + path;
            if ("GET".equals(method)) {
                return client.get().uri(uri).exchange();
            }
            return client.post().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(payload).exchange();
        }
    }

    private static final class RecordingMarketingPolicyFacade implements MarketingPolicyFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private String lastUserId;
        private String lastChannel;
        private ConsentCommand lastConsentCommand;
        private boolean failConsent;

        @Override
        public PolicyState policyState(Long tenantId, String userId, String channel) {
            operations.add("policyState");
            lastTenantId = tenantId;
            lastUserId = userId;
            lastChannel = channel;
            return new PolicyState(userId, channel, null, List.of(), null);
        }

        @Override
        public ConsentView upsertConsent(Long tenantId, ConsentCommand command) {
            operations.add("upsertConsent");
            lastTenantId = tenantId;
            lastConsentCommand = command;
            if (failConsent) {
                throw new IllegalArgumentException("userId is required");
            }
            return new ConsentView(1L, tenantId, command.userId(), command.channel(), command.consentStatus(),
                    command.source());
        }

        @Override
        public SuppressionView upsertSuppression(Long tenantId, SuppressionCommand command) {
            operations.add("upsertSuppression");
            lastTenantId = tenantId;
            return new SuppressionView(2L, tenantId, command.userId(), command.channel(), command.reason(),
                    Boolean.FALSE.equals(command.active()) ? 0 : 1, command.expiresAt());
        }

        @Override
        public ChannelView upsertChannel(Long tenantId, ChannelCommand command) {
            operations.add("upsertChannel");
            lastTenantId = tenantId;
            return new ChannelView(3L, tenantId, command.userId(), command.channel(), command.address(),
                    command.enabled() == null ? 1 : command.enabled(),
                    command.verified() == null ? 0 : command.verified(), command.metadata());
        }
    }
}
