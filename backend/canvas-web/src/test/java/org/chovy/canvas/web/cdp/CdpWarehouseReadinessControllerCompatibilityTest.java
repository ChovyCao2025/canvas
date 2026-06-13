package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.chovy.canvas.cdp.api.CdpWarehouseReadinessFacade;
import org.chovy.canvas.cdp.api.CdpWarehouseReadinessSectionView;
import org.chovy.canvas.cdp.api.CdpWarehouseReadinessView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseReadinessControllerCompatibilityTest {

    @Test
    void getReadinessWrapsFacadeResponseAndDerivesCompatibilityFields() {
        RecordingReadinessFacade facade = new RecordingReadinessFacade();
        facade.view = new CdpWarehouseReadinessView(
                42L,
                "FAIL",
                LocalDateTime.parse("2026-06-12T10:00:00"),
                List.of(
                        new CdpWarehouseReadinessSectionView("offline_sync", "PASS", "ok"),
                        new CdpWarehouseReadinessSectionView(
                                "realtime_pipelines",
                                "FAIL",
                                "realtime pipeline/job(s) failed"),
                        new CdpWarehouseReadinessSectionView("incidents", "WARN", "open incident")));

        webClient(facade)
                .get()
                .uri("/warehouse/readiness")
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
                .jsonPath("$.data.status").isEqualTo("FAIL")
                .jsonPath("$.data.generatedAt[0]").isEqualTo(2026)
                .jsonPath("$.data.generatedAt[1]").isEqualTo(6)
                .jsonPath("$.data.generatedAt[2]").isEqualTo(12)
                .jsonPath("$.data.generatedAt[3]").isEqualTo(10)
                .jsonPath("$.data.generatedAt[4]").isEqualTo(0)
                .jsonPath("$.data.sections.length()").isEqualTo(3)
                .jsonPath("$.data.sections[0].key").isEqualTo("offline_sync")
                .jsonPath("$.data.sections[0].status").isEqualTo("PASS")
                .jsonPath("$.data.productionReady").isEqualTo(false)
                .jsonPath("$.data.blockerCount").isEqualTo(2)
                .jsonPath("$.data.blockers[0].section").isEqualTo("realtime_pipelines")
                .jsonPath("$.data.blockers[0].status").isEqualTo("FAIL")
                .jsonPath("$.data.blockers[0].reason").value(reason ->
                        assertThat((String) reason).contains("realtime pipeline/job(s) failed"))
                .jsonPath("$.data.blockers[1].section").isEqualTo("incidents")
                .jsonPath("$.data.blockers[1].status").isEqualTo("WARN")
                .jsonPath("$.data.blockers[1].reason").isEqualTo("open incident");

        assertThat(facade.calls).containsExactly(42L);
    }

    private static WebTestClient webClient(CdpWarehouseReadinessFacade facade) {
        return WebTestClient.bindToController(new CdpWarehouseReadinessController(facade)).build();
    }

    private static final class RecordingReadinessFacade implements CdpWarehouseReadinessFacade {
        private final java.util.ArrayList<Long> calls = new java.util.ArrayList<>();
        private CdpWarehouseReadinessView view;

        @Override
        public CdpWarehouseReadinessView readiness(Long tenantId) {
            calls.add(tenantId);
            return view;
        }
    }
}
