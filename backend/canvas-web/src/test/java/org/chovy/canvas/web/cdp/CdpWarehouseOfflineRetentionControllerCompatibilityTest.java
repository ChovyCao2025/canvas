package org.chovy.canvas.web.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseOfflineRetentionFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CdpWarehouseOfflineRetentionControllerCompatibilityTest {

    @Test
    void exposesOfflineCycleRoutesWithLegacyEnvelopeAndQueryBodyDefaults() {
        RecordingFacade facade = new RecordingFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/warehouse/offline-cycle/plan?backfillLimit=1200&aggregationWindowMinutes=45"
                        + "&now=2026-06-15T07:00:00")
                .header("X-Tenant-Id", "42")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.tenantId").isEqualTo(42)
                .jsonPath("$.data.backfillLimit").isEqualTo(1200)
                .jsonPath("$.data.aggregationWindowMinutes").isEqualTo(45)
                .jsonPath("$.data.steps[0].stepKey").isEqualTo("BACKFILL")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        client.post()
                .uri("/warehouse/offline-cycle/run")
                .header("X-Tenant-Id", "42")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "now", "2026-06-15T07:00:00",
                        "backfillLimit", 1300,
                        "aggregationWindowMinutes", 60,
                        "operator", "alice"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.operator").isEqualTo("alice")
                .jsonPath("$.data.status").isEqualTo("SUCCESS");

        assertThat(facade.lastTenantId).isEqualTo(42L);
        assertThat(facade.lastBackfillLimit).isEqualTo(1300);
        assertThat(facade.lastAggregationWindowMinutes).isEqualTo(60);
        assertThat(facade.lastOperator).isEqualTo("alice");
    }

    @Test
    void exposesRetentionRoutesAndMapsInvalidDaysToApi001() {
        RecordingFacade facade = new RecordingFacade();
        facade.failRetentionPlan = true;
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/warehouse/retention/plan")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("syncRunRetentionDays must be between 1 and 3650")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        facade.failRetentionPlan = false;

        client.post()
                .uri("/warehouse/retention/run")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "syncRunRetentionDays", 45,
                        "realtimeRetryRetentionDays", 20,
                        "resolvedIncidentRetentionDays", 120))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.tenantId").isEqualTo(0)
                .jsonPath("$.data.operator").isEqualTo("warehouse-retention")
                .jsonPath("$.data.totalDeletedRows").isEqualTo(6);

        assertThat(facade.lastTenantId).isEqualTo(0L);
    }

    private static WebTestClient webClient(CdpWarehouseOfflineRetentionFacade facade) {
        return WebTestClient.bindToController(new CdpWarehouseOfflineRetentionController(facade)).build();
    }

    private static final class RecordingFacade implements CdpWarehouseOfflineRetentionFacade {
        private Long lastTenantId;
        private int lastBackfillLimit;
        private int lastAggregationWindowMinutes;
        private String lastOperator;
        private boolean failRetentionPlan;

        @Override
        public OfflineCyclePlanView offlineCyclePlan(Long tenantId, LocalDateTime now, int backfillLimit,
                                                     int aggregationWindowMinutes) {
            lastTenantId = tenantId;
            lastBackfillLimit = backfillLimit;
            lastAggregationWindowMinutes = aggregationWindowMinutes;
            return new OfflineCyclePlanView(tenantId, now, backfillLimit, aggregationWindowMinutes,
                    List.of(new OfflineCycleStepPlanView("BACKFILL", "READY", "ready", 0L, null, null, null)));
        }

        @Override
        public OfflineCycleResultView runOfflineCycle(Long tenantId, LocalDateTime now, int backfillLimit,
                                                      int aggregationWindowMinutes, String operator) {
            lastTenantId = tenantId;
            lastBackfillLimit = backfillLimit;
            lastAggregationWindowMinutes = aggregationWindowMinutes;
            lastOperator = operator;
            return new OfflineCycleResultView(tenantId, now, operator, "SUCCESS", 10L, 0L, 10L, null, List.of());
        }

        @Override
        public RetentionPlanView retentionPlan(Long tenantId, LocalDateTime now, int syncRunRetentionDays,
                                               int realtimeRetryRetentionDays,
                                               int resolvedIncidentRetentionDays) {
            if (failRetentionPlan) {
                throw new IllegalArgumentException("syncRunRetentionDays must be between 1 and 3650");
            }
            LocalDateTime effectiveNow = now == null ? LocalDateTime.of(2026, 6, 15, 7, 0) : now;
            return new RetentionPlanView(tenantId, effectiveNow,
                    targetPlan("SYNC_RUNS", syncRunRetentionDays, effectiveNow.minusDays(syncRunRetentionDays), 2L),
                    targetPlan("REALTIME_RETRIES", realtimeRetryRetentionDays,
                            effectiveNow.minusDays(realtimeRetryRetentionDays), 3L),
                    targetPlan("RESOLVED_INCIDENTS", resolvedIncidentRetentionDays,
                            effectiveNow.minusDays(resolvedIncidentRetentionDays), 1L),
                    6L);
        }

        @Override
        public RetentionCleanupResultView runRetention(Long tenantId, LocalDateTime now, int syncRunRetentionDays,
                                                       int realtimeRetryRetentionDays,
                                                       int resolvedIncidentRetentionDays, String operator) {
            lastTenantId = tenantId;
            LocalDateTime effectiveNow = now == null ? LocalDateTime.of(2026, 6, 15, 7, 0) : now;
            return new RetentionCleanupResultView(tenantId, effectiveNow,
                    operator == null || operator.isBlank() ? "warehouse-retention" : operator,
                    targetResult("SYNC_RUNS", syncRunRetentionDays, effectiveNow.minusDays(syncRunRetentionDays), 2L, 2),
                    targetResult("REALTIME_RETRIES", realtimeRetryRetentionDays,
                            effectiveNow.minusDays(realtimeRetryRetentionDays), 3L, 3),
                    targetResult("RESOLVED_INCIDENTS", resolvedIncidentRetentionDays,
                            effectiveNow.minusDays(resolvedIncidentRetentionDays), 1L, 1),
                    6L);
        }

        private static RetentionTargetPlanView targetPlan(String key, int days, LocalDateTime cutoff, long rows) {
            return new RetentionTargetPlanView(key, days, cutoff, rows, key + " rule");
        }

        private static RetentionTargetResultView targetResult(
                String key, int days, LocalDateTime cutoff, long rows, int deletedRows) {
            return new RetentionTargetResultView(key, days, cutoff, rows, deletedRows);
        }
    }
}
