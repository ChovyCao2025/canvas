package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.cdp.api.CdpWarehouseReadinessFacade;
import org.chovy.canvas.cdp.api.CdpWarehouseReadinessSectionView;
import org.chovy.canvas.cdp.api.CdpWarehouseReadinessView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseRealtimeCutoverReadinessControllerCompatibilityTest {

    @Test
    void cutoverReadinessEchoesLegacyQueryAndBlocksOnNonPassSections() {
        RecordingReadinessFacade facade = new RecordingReadinessFacade();
        facade.view = new CdpWarehouseReadinessView(
                42L,
                "WARN",
                LocalDateTime.parse("2026-06-12T10:00:00"),
                List.of(
                        new CdpWarehouseReadinessSectionView("offline_sync", "PASS", "ok"),
                        new CdpWarehouseReadinessSectionView(
                                "realtime_pipelines",
                                "WARN",
                                "realtime pipeline lag exceeds threshold"),
                        new CdpWarehouseReadinessSectionView("incidents", "FAIL", "open incident")));

        webClient(facade)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/warehouse/realtime/cutover-readiness")
                        .queryParam("targetMode", "DUAL_RUN")
                        .queryParam("pipelineKey", "orders")
                        .queryParam("pipelineKey", "profiles")
                        .queryParam("contractKey", "warehouse-orders-v2")
                        .queryParam("certificationMode", "STRICT")
                        .queryParam("maxCertificationAgeMinutes", "15")
                        .build())
                .header("X-Tenant-Id", "42")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(42)
                .jsonPath("$.data.targetMode").isEqualTo("DUAL_RUN")
                .jsonPath("$.data.pipelineKeys[0]").isEqualTo("orders")
                .jsonPath("$.data.pipelineKeys[1]").isEqualTo("profiles")
                .jsonPath("$.data.contractKeys[0]").isEqualTo("warehouse-orders-v2")
                .jsonPath("$.data.certificationMode").isEqualTo("STRICT")
                .jsonPath("$.data.maxCertificationAgeMinutes").isEqualTo(15)
                .jsonPath("$.data.status").isEqualTo("WARN")
                .jsonPath("$.data.ready").isEqualTo(false)
                .jsonPath("$.data.productionReady").isEqualTo(false)
                .jsonPath("$.data.cutoverAllowed").isEqualTo(false)
                .jsonPath("$.data.blockerCount").isEqualTo(2)
                .jsonPath("$.data.blockers[0].section").isEqualTo("realtime_pipelines")
                .jsonPath("$.data.blockers[0].status").isEqualTo("WARN")
                .jsonPath("$.data.blockers[0].reason").value(reason ->
                        assertThat((String) reason).contains("lag exceeds threshold"))
                .jsonPath("$.data.blockers[1].section").isEqualTo("incidents")
                .jsonPath("$.data.blockers[1].status").isEqualTo("FAIL")
                .jsonPath("$.data.blockers[1].reason").isEqualTo("open incident");

        assertThat(facade.calls).containsExactly(42L);
    }

    @Test
    void cutoverReadinessDefaultsLegacyQueryAndTenantWhenMissing() {
        RecordingReadinessFacade facade = new RecordingReadinessFacade();
        facade.view = new CdpWarehouseReadinessView(
                7L,
                "PASS",
                LocalDateTime.parse("2026-06-12T10:00:00"),
                List.of(new CdpWarehouseReadinessSectionView("realtime_pipelines", "PASS", "ok")));

        webClient(facade)
                .get()
                .uri("/warehouse/realtime/cutover-readiness")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(7)
                .jsonPath("$.data.targetMode").isEqualTo("FLINK_FIRST")
                .jsonPath("$.data.pipelineKeys.length()").isEqualTo(0)
                .jsonPath("$.data.contractKeys.length()").isEqualTo(0)
                .jsonPath("$.data.certificationMode").isEqualTo("HYBRID")
                .jsonPath("$.data.maxCertificationAgeMinutes").isEqualTo(60)
                .jsonPath("$.data.status").isEqualTo("PASS")
                .jsonPath("$.data.ready").isEqualTo(true)
                .jsonPath("$.data.productionReady").isEqualTo(true)
                .jsonPath("$.data.cutoverAllowed").isEqualTo(true)
                .jsonPath("$.data.blockerCount").isEqualTo(0);

        assertThat(facade.calls).containsExactly(7L);
    }

    private static WebTestClient webClient(CdpWarehouseReadinessFacade facade) {
        return WebTestClient
                .bindToController(new CdpWarehouseRealtimeCutoverReadinessController(facade))
                .build();
    }

    private static final class RecordingReadinessFacade implements CdpWarehouseReadinessFacade {
        private final ArrayList<Long> calls = new ArrayList<>();
        private CdpWarehouseReadinessView view;

        @Override
        public CdpWarehouseReadinessView readiness(Long tenantId) {
            calls.add(tenantId);
            return view;
        }
    }
}
