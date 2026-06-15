package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpIdentityTypeFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpIdentityTypeControllerCompatibilityTest {

    @Test
    void exposesLegacyIdentityTypeRoutesWithPageResultEnvelopeAndParameterMapping() {
        RecordingIdentityTypeFacade facade = new RecordingIdentityTypeFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/canvas/identity-types?enabled=1&allowImport=0")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.total").isEqualTo(1)
                .jsonPath("$.data.list[0].code").isEqualTo("email");

        assertThat(facade.lastEnabled).isEqualTo(1);
        assertThat(facade.lastAllowImport).isEqualTo(0);

        client.post()
                .uri("/canvas/identity-types")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("code", " EMAIL ", "name", " Email "))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.code").isEqualTo("created");

        assertThat(facade.lastPayload).containsEntry("code", " EMAIL ");

        client.put()
                .uri("/canvas/identity-types/12")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("id", 99, "code", "member_id", "allowImport", 0, "priority", 5))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.id").isEqualTo(12)
                .jsonPath("$.data.allowImport").isEqualTo(0)
                .jsonPath("$.data.priority").isEqualTo(5);

        assertThat(facade.lastId).isEqualTo(12L);
        assertThat(facade.lastPayload).containsEntry("allowImport", 0).containsEntry("priority", 5);

        client.delete()
                .uri("/canvas/identity-types/12")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.deleted").isEqualTo(true)
                .jsonPath("$.data.id").isEqualTo(12);

        assertThat(facade.lastId).isEqualTo(12L);
        assertThat(facade.operations).containsExactly("list", "create", "update", "delete");
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingIdentityTypeFacade facade = new RecordingIdentityTypeFacade();
        facade.failure = new IllegalArgumentException("identity type not found: 99");

        webClient(facade)
                .delete()
                .uri("/canvas/identity-types/99")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("identity type not found: 99")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpIdentityTypeFacade facade) {
        return WebTestClient.bindToController(new CdpIdentityTypeController(facade)).build();
    }

    private static final class RecordingIdentityTypeFacade implements CdpIdentityTypeFacade {
        private final List<String> operations = new java.util.ArrayList<>();
        private Integer lastEnabled;
        private Integer lastAllowImport;
        private Long lastId;
        private Map<String, Object> lastPayload = Map.of();
        private IllegalArgumentException failure;

        @Override
        public Map<String, Object> list(Integer enabled, Integer allowImport) {
            failIfConfigured();
            operations.add("list");
            lastEnabled = enabled;
            lastAllowImport = allowImport;
            return Map.of("total", 1L, "list", List.of(row(1L, "email")));
        }

        @Override
        public Map<String, Object> create(Map<String, Object> payload) {
            failIfConfigured();
            operations.add("create");
            lastPayload = new LinkedHashMap<>(payload);
            return row(2L, "created");
        }

        @Override
        public Map<String, Object> update(Long id, Map<String, Object> payload) {
            failIfConfigured();
            operations.add("update");
            lastId = id;
            lastPayload = new LinkedHashMap<>(payload);
            Map<String, Object> row = row(id, "updated");
            row.put("allowImport", payload.get("allowImport"));
            row.put("priority", payload.get("priority"));
            return row;
        }

        @Override
        public Map<String, Object> delete(Long id) {
            failIfConfigured();
            operations.add("delete");
            lastId = id;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", id);
            row.put("deleted", true);
            return row;
        }

        private void failIfConfigured() {
            if (failure != null) {
                throw failure;
            }
        }

        private static Map<String, Object> row(Long id, String code) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", id);
            row.put("code", code);
            row.put("name", "Email");
            return row;
        }
    }
}
