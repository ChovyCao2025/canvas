package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseOperationsFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseOperationsControllerCompatibilityTest {

    @Test
    void statusMapsLegacyQueryAndTenantHeader() {
        RecordingFacade facade = new RecordingFacade();

        webClient(facade)
                .get()
                .uri("/warehouse/status?limit=20")
                .header("X-Tenant-Id", "9")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(9)
                .jsonPath("$.data.limit").isEqualTo(20)
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        assertThat(facade.lastTenantId).isEqualTo(9L);
        assertThat(facade.lastLimit).isEqualTo(20);
    }

    @Test
    void backfillMapsLegacyBodyFieldsAndDefaultsTenant() {
        RecordingFacade facade = new RecordingFacade();

        webClient(facade)
                .post()
                .uri("/warehouse/backfill")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("lastId", 10L, "limit", 100, "operator", "alice"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(0)
                .jsonPath("$.data.lastId").isEqualTo(10)
                .jsonPath("$.data.limit").isEqualTo(100)
                .jsonPath("$.data.operator").isEqualTo("alice");

        assertThat(facade.lastTenantId).isEqualTo(0L);
        assertThat(facade.lastLastId).isEqualTo(10L);
        assertThat(facade.lastLimit).isEqualTo(100);
        assertThat(facade.lastOperator).isEqualTo("alice");
    }

    @Test
    void aggregateMapsLegacyDateBodyAndIllegalArgumentsToApi001() {
        RecordingFacade facade = new RecordingFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/warehouse/aggregate")
                .header("X-Tenant-Id", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "from", "2026-06-15T08:00:00",
                        "to", "2026-06-15T09:00:00",
                        "operator", "bob"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(7)
                .jsonPath("$.data.from[0]").isEqualTo(2026)
                .jsonPath("$.data.from[1]").isEqualTo(6)
                .jsonPath("$.data.from[2]").isEqualTo(15)
                .jsonPath("$.data.from[3]").isEqualTo(8)
                .jsonPath("$.data.to[0]").isEqualTo(2026)
                .jsonPath("$.data.to[1]").isEqualTo(6)
                .jsonPath("$.data.to[2]").isEqualTo(15)
                .jsonPath("$.data.to[3]").isEqualTo(9)
                .jsonPath("$.data.operator").isEqualTo("bob");

        assertThat(facade.lastFrom).isEqualTo(LocalDateTime.of(2026, 6, 15, 8, 0));
        assertThat(facade.lastTo).isEqualTo(LocalDateTime.of(2026, 6, 15, 9, 0));

        facade.failAggregate = true;

        client.post()
                .uri("/warehouse/aggregate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("from and to are required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CdpWarehouseOperationsFacade facade) {
        return WebTestClient.bindToController(new CdpWarehouseOperationsController(facade)).build();
    }

    private static final class RecordingFacade implements CdpWarehouseOperationsFacade {
        private Long lastTenantId;
        private Long lastLastId;
        private int lastLimit;
        private String lastOperator;
        private LocalDateTime lastFrom;
        private LocalDateTime lastTo;
        private boolean failAggregate;

        @Override
        public Map<String, Object> status(Long tenantId, int limit) {
            lastTenantId = tenantId;
            lastLimit = limit;
            return ordered("tenantId", tenantId, "limit", limit);
        }

        @Override
        public Map<String, Object> triggerBackfill(Long tenantId, Long lastId, int limit, String operator) {
            lastTenantId = tenantId;
            lastLastId = lastId;
            lastLimit = limit;
            lastOperator = operator;
            return ordered("tenantId", tenantId, "lastId", lastId, "limit", limit, "operator", operator);
        }

        @Override
        public Map<String, Object> triggerAggregation(
                Long tenantId, LocalDateTime from, LocalDateTime to, String operator) {
            if (failAggregate) {
                throw new IllegalArgumentException("from and to are required");
            }
            lastTenantId = tenantId;
            lastFrom = from;
            lastTo = to;
            lastOperator = operator;
            return ordered("tenantId", tenantId, "from", from, "to", to, "operator", operator);
        }

        private static Map<String, Object> ordered(
                String firstKey, Object firstValue,
                String secondKey, Object secondValue) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put(firstKey, firstValue);
            result.put(secondKey, secondValue);
            return result;
        }

        private static Map<String, Object> ordered(
                String firstKey, Object firstValue,
                String secondKey, Object secondValue,
                String thirdKey, Object thirdValue,
                String fourthKey, Object fourthValue) {
            Map<String, Object> result = ordered(firstKey, firstValue, secondKey, secondValue);
            result.put(thirdKey, thirdValue);
            result.put(fourthKey, fourthValue);
            return result;
        }
    }
}
