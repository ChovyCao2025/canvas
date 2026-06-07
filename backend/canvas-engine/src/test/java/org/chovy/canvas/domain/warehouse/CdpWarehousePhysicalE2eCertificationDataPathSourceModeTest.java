package org.chovy.canvas.domain.warehouse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehousePhysicalE2eCertificationDataPathSourceModeTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 6, 5, 10, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 6, 5, 11, 0);

    @Test
    void realtimeDataPathProofUsesMysqlCdcSourceMode() {
        CdpWarehouseProductionReadinessProofService readiness = mock(CdpWarehouseProductionReadinessProofService.class);
        CdpWarehouseTableGovernanceService tableGovernance = mock(CdpWarehouseTableGovernanceService.class);
        CdpWarehouseRealtimePipelineService pipelineService = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseRealtimeJobControlService jobService = mock(CdpWarehouseRealtimeJobControlService.class);
        CdpWarehouseSyntheticDataPathProbeService probeService = mock(CdpWarehouseSyntheticDataPathProbeService.class);
        JdbcTemplate doris = mock(JdbcTemplate.class);

        when(readiness.proof(9L, FROM, TO, "HYBRID", List.of("audience_12")))
                .thenReturn(readinessProof());
        when(doris.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(tableGovernance.inspectLiveAll(9L, "warehouse-e2e-certification"))
                .thenReturn(inspectionSummary());
        when(pipelineService.status(9L, 5))
                .thenReturn(new CdpWarehouseRealtimePipelineService.PipelineStatusSummary(
                        9L, 1, 1, 0, 0, List.of()));
        when(jobService.status(9L, null, 300, 100))
                .thenReturn(new CdpWarehouseRealtimeJobControlService.JobStatusSummary(
                        9L, 1, 1, 0, 0, List.of()));
        when(probeService.run(eq(9L), any(CdpWarehouseSyntheticDataPathProbeService.RunCommand.class)))
                .thenReturn(probeView("PASS", "MYSQL_CDC", "PASS", "SKIPPED", "PASS", 1L));

        CdpWarehousePhysicalE2eCertificationService service =
                new CdpWarehousePhysicalE2eCertificationService(
                        readiness,
                        provider(doris),
                        tableGovernance,
                        provider(pipelineService),
                        provider(jobService),
                        provider(probeService));

        CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification =
                service.certify(9L, FROM, TO, "HYBRID", List.of("audience_12"), true, true, true);

        assertThat(certification.status()).isEqualTo("PASS");
        assertThat(certification.dataPathProof().sourceMode()).isEqualTo("MYSQL_CDC");
        assertThat(certification.evidence()).filteredOn(row -> row.key().equals("synthetic_ods_data_path"))
                .singleElement()
                .satisfies(row -> assertThat(row.reason()).contains("sourceMode=MYSQL_CDC"));
        verify(probeService).run(eq(9L), argThat(command ->
                "e2e-certification".equals(command.probeKey())
                        && Boolean.TRUE.equals(command.strict())
                        && Integer.valueOf(3).equals(command.verifyAttempts())
                        && Integer.valueOf(100).equals(command.verifyDelayMs())
                        && "MYSQL_CDC".equals(command.sourceMode())));
    }

    private CdpWarehouseProductionReadinessProofService.ProductionReadinessProof readinessProof() {
        return new CdpWarehouseProductionReadinessProofService.ProductionReadinessProof(
                9L,
                "PASS",
                TO.plusMinutes(1),
                FROM,
                TO,
                "HYBRID",
                List.of(new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                        "warehouse_readiness", "PASS", "ok")),
                null,
                null,
                List.of(),
                null);
    }

    private CdpWarehouseTableGovernanceService.InspectionSummary inspectionSummary() {
        return new CdpWarehouseTableGovernanceService.InspectionSummary(9L, 1, 1, 0, 0, List.of(
                new CdpWarehouseTableGovernanceService.InspectionReport(
                        1L,
                        9L,
                        "cdp_user_event_fact",
                        "canvas_dwd.cdp_user_event_fact",
                        "PASS",
                        8,
                        0,
                        List.of(),
                        "PASS",
                        "LIVE:SHOW_CREATE_TABLE",
                        TO)));
    }

    private CdpWarehouseSyntheticDataPathProbeService.ProbeRunView probeView(String status,
                                                                             String sourceMode,
                                                                             String sourceStatus,
                                                                             String sinkStatus,
                                                                             String odsStatus,
                                                                             long odsRows) {
        return new CdpWarehouseSyntheticDataPathProbeService.ProbeRunView(
                1L,
                9L,
                "e2e-certification",
                sourceMode,
                "warehouse-probe-1",
                "__warehouse_probe__",
                "__warehouse_probe_user_1",
                true,
                status,
                sourceStatus,
                sinkStatus,
                odsStatus,
                odsRows,
                FROM,
                TO,
                null,
                "[]",
                FROM,
                TO);
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
