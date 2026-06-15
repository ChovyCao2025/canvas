package org.chovy.canvas.web.channels;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.ChannelConnectorFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class ChannelConnectorControllerCompatibilityTest {

    @Test
    void exposesAllLegacyChannelConnectorRoutesWithCompatibilityEnvelope() {
        RecordingChannelConnectorFacade facade = new RecordingChannelConnectorFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                get("", "connectors"),
                get("/limits", "limits"),
                post("/1002/mode", "updateMode", Map.of("mode", "DISABLED", "reason", "maintenance")),
                post("/1002/health-test", "healthTest", Map.of()),
                post("/fallback/validate", "validateFallback", Map.of(
                        "channel", "WHATSAPP",
                        "provider", "META",
                        "fallbackChannel", "SMS",
                        "fallbackProvider", "TWILIO")),
                get("/fallback/decisions", "fallbackDecisions"),
                get("/dedupe-records", "dedupeRecords"));

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
    void mapsHeadersPathVariablesAndPayloadsToFacade() {
        RecordingChannelConnectorFacade facade = new RecordingChannelConnectorFacade();

        webClient(facade)
                .post()
                .uri("/channels/connectors/1002/mode")
                .header("X-Tenant-Id", "42")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("mode", " sandbox ", "reason", "ready"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.id").isEqualTo(1002)
                .jsonPath("$.data.tenantId").isEqualTo(42)
                .jsonPath("$.data.mode").isEqualTo("SANDBOX")
                .jsonPath("$.data.reason").isEqualTo("ready");

        assertThat(facade.lastTenantId).isEqualTo(42L);
        assertThat(facade.lastConnectorId).isEqualTo(1002L);
        assertThat(facade.lastPayload)
                .containsEntry("mode", " sandbox ")
                .containsEntry("reason", "ready");

        webClient(facade)
                .post()
                .uri("/channels/connectors/fallback/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "channel", "email",
                        "provider", "sendgrid",
                        "fallbackChannel", "sms",
                        "fallbackProvider", "twilio"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.valid").isEqualTo(true)
                .jsonPath("$.data.message").isEqualTo("ok");

        assertThat(facade.lastTenantId).isEqualTo(7L);
        assertThat(facade.lastPayload)
                .containsEntry("channel", "email")
                .containsEntry("fallbackProvider", "twilio");
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingChannelConnectorFacade facade = new RecordingChannelConnectorFacade();
        facade.failModeUpdate = true;

        webClient(facade)
                .post()
                .uri("/channels/connectors/1002/mode")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("mode", "real"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("reason is required when disabling a connector")
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(ChannelConnectorFacade facade) {
        return WebTestClient.bindToController(new ChannelConnectorController(facade)).build();
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe("GET", path, operation, Map.of());
    }

    private static RouteProbe post(String path, String operation, Map<String, Object> body) {
        return new RouteProbe("POST", path, operation, body);
    }

    private record RouteProbe(String method, String path, String operation, Map<String, Object> body) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            if ("GET".equals(method)) {
                return client.get().uri("/channels/connectors" + path).exchange();
            }
            WebTestClient.RequestBodySpec request = client.post().uri("/channels/connectors" + path);
            if (body.isEmpty()) {
                return request.exchange();
            }
            return request.contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
        }
    }

    private static final class RecordingChannelConnectorFacade implements ChannelConnectorFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private Long lastConnectorId;
        private Map<String, Object> lastPayload = Map.of();
        private boolean failModeUpdate;

        @Override
        public List<Map<String, Object>> connectors(Long tenantId) {
            operations.add("connectors");
            lastTenantId = tenantId;
            return List.of(row("id", 1001L, "connectorKey", "email-sendgrid", "channel", "EMAIL"));
        }

        @Override
        public List<Map<String, Object>> limits(Long tenantId) {
            operations.add("limits");
            lastTenantId = tenantId;
            return List.of(row("channel", "EMAIL", "provider", "SENDGRID", "operation", "SEND"));
        }

        @Override
        public Map<String, Object> updateMode(Long tenantId, Long connectorId, Map<String, Object> payload,
                                              String actor) {
            operations.add("updateMode");
            capture(tenantId, connectorId, payload);
            if (failModeUpdate) {
                throw new IllegalArgumentException("reason is required when disabling a connector");
            }
            return row(
                    "tenantId", tenantId,
                    "id", connectorId,
                    "mode", String.valueOf(payload.get("mode")).trim().toUpperCase(),
                    "reason", payload.get("reason"));
        }

        @Override
        public Map<String, Object> healthTest(Long tenantId, Long connectorId) {
            operations.add("healthTest");
            lastTenantId = tenantId;
            lastConnectorId = connectorId;
            return row("status", "UP", "message", "sandbox connector ready");
        }

        @Override
        public Map<String, Object> validateFallback(Long tenantId, Map<String, Object> payload) {
            operations.add("validateFallback");
            capture(tenantId, null, payload);
            return row("valid", true, "message", "ok");
        }

        @Override
        public List<Map<String, Object>> fallbackDecisions(Long tenantId) {
            operations.add("fallbackDecisions");
            lastTenantId = tenantId;
            return List.of(row("originalChannel", "WHATSAPP", "finalChannel", "SMS"));
        }

        @Override
        public List<Map<String, Object>> dedupeRecords(Long tenantId) {
            operations.add("dedupeRecords");
            lastTenantId = tenantId;
            return List.of(row("dedupeGroup", "campaign-101", "contentHash", "sha256:demo"));
        }

        private void capture(Long tenantId, Long connectorId, Map<String, Object> payload) {
            lastTenantId = tenantId;
            lastConnectorId = connectorId;
            lastPayload = new LinkedHashMap<>(payload);
        }

        private static Map<String, Object> row(Object... keysAndValues) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < keysAndValues.length; i += 2) {
                row.put((String) keysAndValues[i], keysAndValues[i + 1]);
            }
            return row;
        }
    }
}
