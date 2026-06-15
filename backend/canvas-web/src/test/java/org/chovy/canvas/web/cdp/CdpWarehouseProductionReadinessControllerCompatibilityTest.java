package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseProductionReadinessFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseProductionReadinessControllerCompatibilityTest {

    @Test
    void productionReadinessMapsLegacyQueryAndReturnsSuccessEnvelope() {
        RecordingFacade facade = new RecordingFacade();

        webClient(facade)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/warehouse/production-readiness")
                        .queryParam("from", "2026-06-05T10:00:00")
                        .queryParam("to", "2026-06-05T11:00:00")
                        .queryParam("mode", "HYBRID")
                        .queryParam("contractKey", "a")
                        .queryParam("contractKey", "b")
                        .build())
                .header("X-Tenant-Id", "42")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.tenantId").isEqualTo(42)
                .jsonPath("$.data.status").isEqualTo("PASS")
                .jsonPath("$.data.windowStart[0]").isEqualTo(2026)
                .jsonPath("$.data.windowStart[1]").isEqualTo(6)
                .jsonPath("$.data.windowStart[2]").isEqualTo(5)
                .jsonPath("$.data.windowStart[3]").isEqualTo(10)
                .jsonPath("$.data.windowEnd[3]").isEqualTo(11)
                .jsonPath("$.data.mode").isEqualTo("HYBRID")
                .jsonPath("$.data.evidence[0].key").isEqualTo("warehouse_readiness")
                .jsonPath("$.data.evidence[0].status").isEqualTo("PASS")
                .jsonPath("$.data.contracts[0].contractKey").isEqualTo("a")
                .jsonPath("$.data.contracts[1].contractKey").isEqualTo("b");

        assertThat(facade.calls).hasSize(1);
        Call call = facade.calls.get(0);
        assertThat(call.tenantId()).isEqualTo(42L);
        assertThat(call.from()).isEqualTo(LocalDateTime.parse("2026-06-05T10:00:00"));
        assertThat(call.to()).isEqualTo(LocalDateTime.parse("2026-06-05T11:00:00"));
        assertThat(call.mode()).isEqualTo("HYBRID");
        assertThat(call.contractKeys()).containsExactly("a", "b");
    }

    @Test
    void productionReadinessDefaultsTenantModeAndContractKeysWhenMissing() {
        RecordingFacade facade = new RecordingFacade();

        webClient(facade)
                .get()
                .uri("/warehouse/production-readiness")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(0)
                .jsonPath("$.data.mode").isEqualTo("HYBRID")
                .jsonPath("$.data.contracts.length()").isEqualTo(0);

        assertThat(facade.calls).singleElement().satisfies(call -> {
            assertThat(call.tenantId()).isZero();
            assertThat(call.from()).isNull();
            assertThat(call.to()).isNull();
            assertThat(call.mode()).isEqualTo("HYBRID");
            assertThat(call.contractKeys()).isEmpty();
        });
    }

    private static WebTestClient webClient(CdpWarehouseProductionReadinessFacade facade) {
        return WebTestClient
                .bindToController(new CdpWarehouseProductionReadinessController(facade))
                .build();
    }

    private record Call(
            Long tenantId,
            LocalDateTime from,
            LocalDateTime to,
            String mode,
            List<String> contractKeys) {
    }

    private static final class RecordingFacade implements CdpWarehouseProductionReadinessFacade {
        private final ArrayList<Call> calls = new ArrayList<>();

        @Override
        public Map<String, Object> proof(
                Long tenantId,
                LocalDateTime from,
                LocalDateTime to,
                String mode,
                List<String> contractKeys) {
            calls.add(new Call(tenantId, from, to, mode, List.copyOf(contractKeys)));
            Map<String, Object> proof = ordered();
            proof.put("tenantId", tenantId);
            proof.put("status", "PASS");
            proof.put("generatedAt", LocalDateTime.parse("2026-06-05T11:01:00"));
            proof.put("windowStart", from == null ? LocalDateTime.parse("2026-06-05T10:00:00") : from);
            proof.put("windowEnd", to == null ? LocalDateTime.parse("2026-06-05T11:00:00") : to);
            proof.put("mode", mode);
            proof.put("evidence", List.of(Map.of(
                    "key", "warehouse_readiness",
                    "status", "PASS",
                    "reason", "ok")));
            proof.put("readiness", null);
            proof.put("availability", null);
            proof.put("contracts", contractKeys.stream()
                    .map(contractKey -> Map.of(
                            "contractKey", contractKey,
                            "status", "PASS",
                            "allowed", true,
                            "reason", "allowed"))
                    .toList());
            proof.put("privacyErasureBacklog", null);
            proof.put("enterpriseOlapReadiness", null);
            return proof;
        }

        private static Map<String, Object> ordered() {
            return new LinkedHashMap<>();
        }
    }
}
