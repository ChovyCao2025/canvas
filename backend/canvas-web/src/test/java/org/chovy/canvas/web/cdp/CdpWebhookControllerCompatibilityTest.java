package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWebhookFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWebhookControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final Long HEADER_TENANT_ID = 42L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String HEADER_ACTOR = "webhook-operator";

    @Test
    void exposesAllLegacyWebhookRoutesWithCompatibilityEnvelope() {
        RecordingWebhookFacade facade = new RecordingWebhookFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                get("", "list"),
                post("", "create", Map.of("name", "Hook", "callbackUrl", "https://hooks.example.test/cdp")),
                put("/101", "update", Map.of("name", "Hook v2", "callbackUrl", "https://hooks.example.test/v2")),
                put("/101/pause", "pause", Map.of()),
                put("/101/resume", "resume", Map.of()),
                delete("/101", "disable"),
                post("/101/rotate-secret", "rotateSecret", Map.of()),
                post("/101/test", "testDelivery", Map.of()),
                get("/101/deliveries", "deliveries"));

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
    void headersDefaultsPathVariablesAndPayloadsAreMappedToFacade() {
        RecordingWebhookFacade facade = new RecordingWebhookFacade();

        webClient(facade)
                .post()
                .uri("/cdp/webhooks")
                .header("X-Tenant-Id", HEADER_TENANT_ID.toString())
                .header("X-Actor", HEADER_ACTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Payment Hook", "callbackUrl", "https://hooks.example.test/payments"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(HEADER_TENANT_ID.intValue())
                .jsonPath("$.data.updatedBy").isEqualTo(HEADER_ACTOR)
                .jsonPath("$.data.payload.name").isEqualTo("Payment Hook");

        assertThat(facade.lastTenantId).isEqualTo(HEADER_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(HEADER_ACTOR);
        assertThat(facade.lastPayload).containsEntry("name", "Payment Hook");

        webClient(facade)
                .post()
                .uri("/cdp/webhooks/101/test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(DEFAULT_TENANT_ID.intValue())
                .jsonPath("$.data.subscriptionId").isEqualTo(101)
                .jsonPath("$.data.triggeredBy").isEqualTo(DEFAULT_ACTOR);

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastActor).isEqualTo(DEFAULT_ACTOR);
        assertThat(facade.lastId).isEqualTo(101L);
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingWebhookFacade facade = new RecordingWebhookFacade();
        facade.failCreate = true;

        webClient(facade)
                .post()
                .uri("/cdp/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("name is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpWebhookFacade facade) {
        return WebTestClient.bindToController(new CdpWebhookController(facade)).build();
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe("GET", path, operation, Map.of());
    }

    private static RouteProbe post(String path, String operation, Map<String, Object> body) {
        return new RouteProbe("POST", path, operation, body);
    }

    private static RouteProbe put(String path, String operation, Map<String, Object> body) {
        return new RouteProbe("PUT", path, operation, body);
    }

    private static RouteProbe delete(String path, String operation) {
        return new RouteProbe("DELETE", path, operation, Map.of());
    }

    private record RouteProbe(String method, String path, String operation, Map<String, Object> body) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            if ("GET".equals(method)) {
                return client.get().uri("/cdp/webhooks" + path).exchange();
            }
            if ("DELETE".equals(method)) {
                return client.delete().uri("/cdp/webhooks" + path).exchange();
            }
            WebTestClient.RequestBodySpec request = "POST".equals(method)
                    ? client.post().uri("/cdp/webhooks" + path)
                    : client.put().uri("/cdp/webhooks" + path);
            if (body.isEmpty()) {
                return request.exchange();
            }
            return request.contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange();
        }
    }

    private static final class RecordingWebhookFacade implements CdpWebhookFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastTenantId;
        private Long lastId;
        private String lastActor;
        private Map<String, Object> lastPayload = Map.of();
        private boolean failCreate;

        @Override
        public Map<String, Object> list(Long tenantId) {
            operations.add("list");
            lastTenantId = tenantId;
            return Map.of("total", 1L, "records", List.of(view(tenantId, 101L, "list", DEFAULT_ACTOR)));
        }

        @Override
        public Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor) {
            operations.add("create");
            if (failCreate) {
                throw new IllegalArgumentException("name is required");
            }
            lastTenantId = tenantId;
            lastActor = actor;
            lastPayload = new LinkedHashMap<>(payload);
            Map<String, Object> row = view(tenantId, 101L, "create", actor);
            row.put("payload", new LinkedHashMap<>(payload));
            return row;
        }

        @Override
        public Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor) {
            operations.add("update");
            lastPayload = new LinkedHashMap<>(payload);
            return subscriptionOperation(tenantId, id, actor);
        }

        @Override
        public Map<String, Object> pause(Long tenantId, Long id, String actor) {
            operations.add("pause");
            return subscriptionOperation(tenantId, id, actor);
        }

        @Override
        public Map<String, Object> resume(Long tenantId, Long id, String actor) {
            operations.add("resume");
            return subscriptionOperation(tenantId, id, actor);
        }

        @Override
        public Map<String, Object> disable(Long tenantId, Long id, String actor) {
            operations.add("disable");
            return subscriptionOperation(tenantId, id, actor);
        }

        @Override
        public Map<String, Object> rotateSecret(Long tenantId, Long id, String actor) {
            operations.add("rotateSecret");
            Map<String, Object> row = subscriptionOperation(tenantId, id, actor);
            row.put("secret", "whsec_test");
            return row;
        }

        @Override
        public Map<String, Object> testDelivery(Long tenantId, Long id, String actor) {
            operations.add("testDelivery");
            Map<String, Object> row = subscriptionOperation(tenantId, id, actor);
            row.put("triggeredBy", actor);
            return row;
        }

        @Override
        public Map<String, Object> deliveries(Long tenantId, Long id, Integer limit) {
            operations.add("deliveries");
            lastTenantId = tenantId;
            lastId = id;
            return Map.of("tenantId", tenantId, "subscriptionId", id, "limit", limit, "records", List.of());
        }

        private Map<String, Object> subscriptionOperation(Long tenantId, Long id, String actor) {
            lastTenantId = tenantId;
            lastId = id;
            lastActor = actor;
            return view(tenantId, id, operations.get(operations.size() - 1), actor);
        }

        private static Map<String, Object> view(Long tenantId, Long id, String operation, String actor) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenantId", tenantId);
            row.put("subscriptionId", id);
            row.put("operation", operation);
            row.put("updatedBy", actor);
            return row;
        }
    }
}
