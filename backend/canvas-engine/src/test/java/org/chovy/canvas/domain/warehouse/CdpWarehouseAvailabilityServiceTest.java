package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CdpWarehouseAvailabilityServiceTest {

    @Test
    void offlineAvailabilityPassesWhenAggregateWatermarkCoversRequestedWindow() {
        CdpWarehouseOperationsService operationsService = mock(CdpWarehouseOperationsService.class);
        when(operationsService.status(9L, 20)).thenReturn(statusWithWatermark(
                LocalDateTime.of(2026, 6, 5, 12, 0)));
        CdpWarehouseAvailabilityService service = service(operationsService,
                mock(CdpWarehouseRealtimePipelineService.class), defaultPolicyService());

        CdpWarehouseAvailabilityService.AvailabilityDecision decision = service.evaluate(
                9L,
                LocalDateTime.of(2026, 6, 5, 10, 0),
                LocalDateTime.of(2026, 6, 5, 11, 0),
                "OFFLINE");

        assertThat(decision.status()).isEqualTo("PASS");
        assertThat(decision.gates()).hasSize(1);
        assertThat(decision.gates().get(0).gateKey()).isEqualTo("offline_aggregate");
        assertThat(decision.gates().get(0).availableUntil()).isEqualTo(
                LocalDateTime.of(2026, 6, 5, 12, 0));
    }

    @Test
    void offlineAvailabilityFailsWhenLagBreachesSloPolicy() {
        CdpWarehouseOperationsService operationsService = mock(CdpWarehouseOperationsService.class);
        when(operationsService.status(9L, 20)).thenReturn(statusWithWatermark(
                LocalDateTime.of(2026, 6, 5, 10, 0)));
        CdpWarehouseAvailabilityService service = service(operationsService,
                mock(CdpWarehouseRealtimePipelineService.class), strictPolicyService());

        CdpWarehouseAvailabilityService.AvailabilityDecision decision = service.evaluate(
                9L,
                LocalDateTime.of(2026, 6, 5, 10, 0),
                LocalDateTime.of(2026, 6, 5, 11, 0),
                "offline");

        assertThat(decision.status()).isEqualTo("FAIL");
        assertThat(decision.gates().get(0).lagMinutes()).isEqualTo(60);
        assertThat(decision.gates().get(0).reason()).contains("past offline aggregate watermark");
    }

    @Test
    void realtimeAvailabilityPassesWhenAllPipelineWatermarksCoverWindow() {
        CdpWarehouseRealtimePipelineService realtimeService =
                mock(CdpWarehouseRealtimePipelineService.class);
        when(realtimeService.status(9L, 3)).thenReturn(realtimeSummary("PASS",
                pipeline("mysql_cdp_event_log_to_doris_ods", LocalDateTime.of(2026, 6, 5, 12, 0), 600_000L),
                pipeline("mysql_canvas_trace_to_doris_ods", LocalDateTime.of(2026, 6, 5, 11, 30), 600_000L)));
        CdpWarehouseAvailabilityService service = service(mock(CdpWarehouseOperationsService.class),
                realtimeService, defaultPolicyService());

        CdpWarehouseAvailabilityService.AvailabilityDecision decision = service.evaluate(
                9L,
                LocalDateTime.of(2026, 6, 5, 10, 0),
                LocalDateTime.of(2026, 6, 5, 11, 0),
                "REALTIME");

        assertThat(decision.status()).isEqualTo("PASS");
        assertThat(decision.gates().get(0).gateKey()).isEqualTo("realtime_pipelines");
        assertThat(decision.gates().get(0).availableUntil()).isEqualTo(
                LocalDateTime.of(2026, 6, 5, 11, 30));
        assertThat(decision.gates().get(0).evidenceCount()).isEqualTo(2);
    }

    @Test
    void realtimeAvailabilityFailsWhenRequestedWindowExceedsRealtimeTolerance() {
        CdpWarehouseRealtimePipelineService realtimeService =
                mock(CdpWarehouseRealtimePipelineService.class);
        when(realtimeService.status(9L, 3)).thenReturn(realtimeSummary("PASS",
                pipeline("mysql_cdp_event_log_to_doris_ods", LocalDateTime.of(2026, 6, 5, 10, 0), 600_000L)));
        CdpWarehouseAvailabilityService service = service(mock(CdpWarehouseOperationsService.class),
                realtimeService, defaultPolicyService());

        CdpWarehouseAvailabilityService.AvailabilityDecision decision = service.evaluate(
                9L,
                LocalDateTime.of(2026, 6, 5, 10, 0),
                LocalDateTime.of(2026, 6, 5, 11, 0),
                "REALTIME");

        assertThat(decision.status()).isEqualTo("FAIL");
        assertThat(decision.gates().get(0).lagMinutes()).isEqualTo(60);
    }

    @Test
    void hybridAvailabilityUsesWorstGateStatus() {
        CdpWarehouseOperationsService operationsService = mock(CdpWarehouseOperationsService.class);
        when(operationsService.status(9L, 20)).thenReturn(statusWithWatermark(
                LocalDateTime.of(2026, 6, 5, 12, 0)));
        CdpWarehouseRealtimePipelineService realtimeService =
                mock(CdpWarehouseRealtimePipelineService.class);
        when(realtimeService.status(9L, 3)).thenReturn(realtimeSummary("WARN",
                pipeline("mysql_cdp_event_log_to_doris_ods", LocalDateTime.of(2026, 6, 5, 12, 0), 600_000L)));
        CdpWarehouseAvailabilityService service = service(operationsService, realtimeService, defaultPolicyService());

        CdpWarehouseAvailabilityService.AvailabilityDecision decision = service.evaluate(
                9L,
                LocalDateTime.of(2026, 6, 5, 10, 0),
                LocalDateTime.of(2026, 6, 5, 11, 0),
                "HYBRID");

        assertThat(decision.status()).isEqualTo("WARN");
        assertThat(decision.gates()).extracting(CdpWarehouseAvailabilityService.AvailabilityGate::gateKey)
                .containsExactly("offline_aggregate", "realtime_pipelines");
    }

    @Test
    void evaluateRejectsInvalidWindowAndMode() {
        CdpWarehouseAvailabilityService service = service(mock(CdpWarehouseOperationsService.class),
                mock(CdpWarehouseRealtimePipelineService.class), defaultPolicyService());

        assertThatThrownBy(() -> service.evaluate(
                9L,
                LocalDateTime.of(2026, 6, 5, 12, 0),
                LocalDateTime.of(2026, 6, 5, 11, 0),
                "OFFLINE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from");
        assertThatThrownBy(() -> service.evaluate(
                9L,
                LocalDateTime.of(2026, 6, 5, 10, 0),
                LocalDateTime.of(2026, 6, 5, 11, 0),
                "MIXED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mode");
    }

    private CdpWarehouseAvailabilityService service(
            CdpWarehouseOperationsService operationsService,
            CdpWarehouseRealtimePipelineService realtimeService,
            CdpWarehouseSloPolicyService policyService) {
        return new CdpWarehouseAvailabilityService(operationsService, realtimeService, policyService);
    }

    private CdpWarehouseOperationsService.WarehouseStatus statusWithWatermark(LocalDateTime watermark) {
        return new CdpWarehouseOperationsService.WarehouseStatus(
                9L,
                List.of(),
                List.of(new CdpWarehouseOperationsService.WatermarkRow(
                        1L,
                        "CDP_EVENT_AGGREGATE",
                        "WINDOW_END",
                        watermark.toString(),
                        watermark,
                        watermark)));
    }

    private CdpWarehouseRealtimePipelineService.PipelineStatusSummary realtimeSummary(
            String status,
            CdpWarehouseRealtimePipelineService.PipelineRuntimeView... pipelines) {
        long passed = "PASS".equals(status) ? pipelines.length : 0;
        long warned = "WARN".equals(status) ? pipelines.length : 0;
        long failed = "FAIL".equals(status) ? pipelines.length : 0;
        return new CdpWarehouseRealtimePipelineService.PipelineStatusSummary(
                9L,
                pipelines.length,
                passed,
                warned,
                failed,
                List.of(pipelines));
    }

    private CdpWarehouseRealtimePipelineService.PipelineRuntimeView pipeline(
            String pipelineKey,
            LocalDateTime watermark,
            Long maxLagMs) {
        CdpWarehouseRealtimePipelineService.PipelineContractView contract =
                new CdpWarehouseRealtimePipelineService.PipelineContractView(
                        1L,
                        9L,
                        pipelineKey,
                        pipelineKey,
                        "MYSQL_CDC",
                        "mysql.table",
                        null,
                        null,
                        "FLINK_CDC",
                        "DORIS",
                        "canvas_ods.table",
                        "AT_LEAST_ONCE",
                        60,
                        maxLagMs,
                        300,
                        "ACTIVE",
                        "data-platform",
                        "{}",
                        "chk-1",
                        "100",
                        "100",
                        watermark,
                        watermark,
                        1000L,
                        "PASS",
                        "ok",
                        "flink");
        return new CdpWarehouseRealtimePipelineService.PipelineRuntimeView(
                contract,
                "PASS",
                "Realtime pipeline healthy",
                List.of(),
                "chk-1",
                "100",
                "100",
                watermark,
                watermark,
                1000L,
                "flink",
                List.of());
    }

    private CdpWarehouseSloPolicyService defaultPolicyService() {
        CdpWarehouseSloPolicyService policyService = mock(CdpWarehouseSloPolicyService.class);
        when(policyService.effectivePolicy(9L)).thenReturn(CdpWarehouseSloPolicyService.defaultPolicy(9L));
        return policyService;
    }

    private CdpWarehouseSloPolicyService strictPolicyService() {
        CdpWarehouseSloPolicyService policyService = mock(CdpWarehouseSloPolicyService.class);
        when(policyService.effectivePolicy(9L)).thenReturn(new CdpWarehouseSloPolicyService.SloPolicyView(
                1L,
                9L,
                "WAREHOUSE_READINESS_DEFAULT",
                "Strict policy",
                10,
                30,
                10,
                30,
                1440,
                4320,
                "ACTIVE",
                "ops",
                "strict"));
        return policyService;
    }
}
