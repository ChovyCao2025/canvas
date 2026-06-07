package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CdpWarehouseRealtimeCutoverReadinessServiceTest {

    @Test
    void flinkFirstPassesOnlyWhenPipelinesAndCertificationGatePass() {
        CdpWarehouseRealtimePipelineService pipelineService = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseE2eCertificationGateService gateService = mock(CdpWarehouseE2eCertificationGateService.class);
        when(pipelineService.status(9L, 20)).thenReturn(pipelineSummary(List.of(
                runtime("mysql_cdp_event_log_to_doris_ods", "PASS"),
                runtime("doris_ods_cdp_event_to_dwd_fact", "PASS"))));
        when(gateService.evaluate(9L, "HYBRID", List.of("audience_12"), true, true, true, 60))
                .thenReturn(gate("PASS", "fresh PASS certification evidence"));
        CdpWarehouseRealtimeCutoverReadinessService service =
                new CdpWarehouseRealtimeCutoverReadinessService(pipelineService, gateService);

        CdpWarehouseRealtimeCutoverReadinessService.CutoverDecision decision = service.evaluate(9L,
                new CdpWarehouseRealtimeCutoverReadinessService.CutoverCommand(
                        "FLINK_FIRST",
                        List.of("mysql_cdp_event_log_to_doris_ods", "doris_ods_cdp_event_to_dwd_fact"),
                        List.of("audience_12"),
                        "HYBRID",
                        60L));

        assertThat(decision.status()).isEqualTo("PASS");
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.gates()).extracting(CdpWarehouseRealtimeCutoverReadinessService.CutoverGate::key)
                .containsExactly("pipeline:mysql_cdp_event_log_to_doris_ods",
                        "pipeline:doris_ods_cdp_event_to_dwd_fact",
                        "e2e_certification");
    }

    @Test
    void flinkFirstFailsWhenRequiredPipelineIsMissing() {
        CdpWarehouseRealtimePipelineService pipelineService = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseE2eCertificationGateService gateService = mock(CdpWarehouseE2eCertificationGateService.class);
        when(pipelineService.status(9L, 20)).thenReturn(pipelineSummary(List.of(
                runtime("mysql_cdp_event_log_to_doris_ods", "PASS"))));
        when(gateService.evaluate(9L, "HYBRID", List.of(), true, true, true, 60))
                .thenReturn(gate("PASS", "fresh PASS certification evidence"));
        CdpWarehouseRealtimeCutoverReadinessService service =
                new CdpWarehouseRealtimeCutoverReadinessService(pipelineService, gateService);

        CdpWarehouseRealtimeCutoverReadinessService.CutoverDecision decision = service.evaluate(9L,
                new CdpWarehouseRealtimeCutoverReadinessService.CutoverCommand(
                        "FLINK_FIRST",
                        List.of("mysql_cdp_event_log_to_doris_ods", "doris_ods_cdp_event_to_dwd_fact"),
                        List.of(),
                        "HYBRID",
                        60L));

        assertThat(decision.status()).isEqualTo("FAIL");
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.gates()).filteredOn(gate -> gate.key().equals("pipeline:doris_ods_cdp_event_to_dwd_fact"))
                .singleElement()
                .satisfies(gate -> {
                    assertThat(gate.status()).isEqualTo("FAIL");
                    assertThat(gate.reason()).contains("required realtime pipeline is missing");
                });
    }

    @Test
    void flinkFirstFailsWhenCertificationGateFails() {
        CdpWarehouseRealtimePipelineService pipelineService = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseE2eCertificationGateService gateService = mock(CdpWarehouseE2eCertificationGateService.class);
        when(pipelineService.status(9L, 20)).thenReturn(pipelineSummary(List.of(
                runtime("mysql_cdp_event_log_to_doris_ods", "PASS"))));
        when(gateService.evaluate(9L, "HYBRID", List.of("audience_12"), true, true, true, 60))
                .thenReturn(gate("FAIL", "requires dataPathProof sourceMode MYSQL_CDC"));
        CdpWarehouseRealtimeCutoverReadinessService service =
                new CdpWarehouseRealtimeCutoverReadinessService(pipelineService, gateService);

        CdpWarehouseRealtimeCutoverReadinessService.CutoverDecision decision = service.evaluate(9L,
                new CdpWarehouseRealtimeCutoverReadinessService.CutoverCommand(
                        "FLINK_FIRST",
                        List.of("mysql_cdp_event_log_to_doris_ods"),
                        List.of("audience_12"),
                        "HYBRID",
                        60L));

        assertThat(decision.status()).isEqualTo("FAIL");
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.gates()).filteredOn(gate -> gate.key().equals("e2e_certification"))
                .singleElement()
                .satisfies(gate -> assertThat(gate.reason()).contains("MYSQL_CDC"));
    }

    @Test
    void directStreamLoadFallbackIsAllowedWithoutFlinkPromotionChecks() {
        CdpWarehouseRealtimePipelineService pipelineService = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseE2eCertificationGateService gateService = mock(CdpWarehouseE2eCertificationGateService.class);
        CdpWarehouseRealtimeCutoverReadinessService service =
                new CdpWarehouseRealtimeCutoverReadinessService(pipelineService, gateService);

        CdpWarehouseRealtimeCutoverReadinessService.CutoverDecision decision = service.evaluate(9L,
                new CdpWarehouseRealtimeCutoverReadinessService.CutoverCommand(
                        "DIRECT_STREAM_LOAD",
                        List.of(),
                        List.of(),
                        "HYBRID",
                        60L));

        assertThat(decision.status()).isEqualTo("PASS");
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.summary()).contains("fallback remains allowed");
        verifyNoInteractions(pipelineService, gateService);
    }

    private CdpWarehouseRealtimePipelineService.PipelineStatusSummary pipelineSummary(
            List<CdpWarehouseRealtimePipelineService.PipelineRuntimeView> rows) {
        long passed = rows.stream().filter(row -> "PASS".equals(row.runtimeStatus())).count();
        long warned = rows.stream().filter(row -> "WARN".equals(row.runtimeStatus())).count();
        long failed = rows.stream().filter(row -> "FAIL".equals(row.runtimeStatus())).count();
        return new CdpWarehouseRealtimePipelineService.PipelineStatusSummary(9L, rows.size(), passed, warned, failed, rows);
    }

    private CdpWarehouseRealtimePipelineService.PipelineRuntimeView runtime(String pipelineKey, String status) {
        return new CdpWarehouseRealtimePipelineService.PipelineRuntimeView(
                new CdpWarehouseRealtimePipelineService.PipelineContractView(
                        1L,
                        9L,
                        pipelineKey,
                        pipelineKey,
                        "MYSQL_CDC",
                        "canvas.cdp_event_log",
                        null,
                        "canvas-cdp",
                        "FLINK_CDC",
                        "DORIS",
                        "canvas_ods.cdp_event_log",
                        "EXACTLY_ONCE",
                        60,
                        300_000L,
                        300,
                        "ACTIVE",
                        "warehouse",
                        null,
                        "chk-1",
                        "binlog:1",
                        "binlog:1",
                        LocalDateTime.parse("2026-06-06T03:55:00"),
                        LocalDateTime.parse("2026-06-06T03:55:10"),
                        100L,
                        status,
                        status.toLowerCase(),
                        "canvas-flink"),
                status,
                status.toLowerCase(),
                List.of(),
                "chk-1",
                "binlog:1",
                "binlog:1",
                LocalDateTime.parse("2026-06-06T03:55:00"),
                LocalDateTime.parse("2026-06-06T03:55:10"),
                100L,
                "canvas-flink",
                List.of());
    }

    private CdpWarehouseE2eCertificationGateService.GateDecision gate(String status, String reason) {
        return new CdpWarehouseE2eCertificationGateService.GateDecision(
                9L,
                status,
                reason,
                7L,
                status,
                LocalDateTime.parse("2026-06-06T03:55:30"),
                LocalDateTime.parse("2026-06-06T04:55:30"),
                "HYBRID",
                true,
                true,
                true,
                60,
                List.of("audience_12"));
    }
}
