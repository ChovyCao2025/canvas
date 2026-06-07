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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CdpWarehousePhysicalE2eCertificationServiceTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 6, 5, 10, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 6, 5, 11, 0);

    @Test
    void certificationWarnsWhenPhysicalProofPassesButRealtimeIsNotConfigured() {
        CdpWarehouseProductionReadinessProofService readiness = mock(CdpWarehouseProductionReadinessProofService.class);
        CdpWarehouseTableGovernanceService tableGovernance = mock(CdpWarehouseTableGovernanceService.class);
        JdbcTemplate doris = mock(JdbcTemplate.class);
        ObjectProvider<JdbcTemplate> dorisProvider = dorisProvider(doris);
        CdpWarehouseProductionReadinessProofService.ProductionReadinessProof proof = readinessProof("PASS");
        CdpWarehouseTableGovernanceService.InspectionSummary liveInspection = inspectionSummary("PASS", 2, 2, 0, 0);
        when(readiness.proof(9L, FROM, TO, "HYBRID", List.of("bi_daily_active_users", "audience_12")))
                .thenReturn(proof);
        when(doris.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(tableGovernance.inspectLiveAll(9L, "warehouse-e2e-certification")).thenReturn(liveInspection);
        CdpWarehousePhysicalE2eCertificationService service =
                new CdpWarehousePhysicalE2eCertificationService(readiness, dorisProvider, tableGovernance);

        CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification =
                service.certify(9L, FROM, TO, "HYBRID",
                        List.of("bi_daily_active_users", "audience_12"), true);

        assertThat(certification.status()).isEqualTo("WARN");
        assertThat(certification.tenantId()).isEqualTo(9L);
        assertThat(certification.requirePhysical()).isTrue();
        assertThat(certification.requireRealtime()).isFalse();
        assertThat(certification.productionReadiness()).isSameAs(proof);
        assertThat(certification.liveTableInspection()).isSameAs(liveInspection);
        assertThat(certification.evidence()).extracting(
                        CdpWarehousePhysicalE2eCertificationService.CertificationEvidence::key)
                .containsExactly(
                        "production_readiness",
                        "doris_jdbc_connectivity",
                        "live_table_contracts",
                        "realtime_pipeline_status",
                        "realtime_job_status");
        verify(readiness).proof(9L, FROM, TO, "HYBRID", List.of("bi_daily_active_users", "audience_12"));
        verify(doris).queryForObject("SELECT 1", Integer.class);
        verify(tableGovernance).inspectLiveAll(9L, "warehouse-e2e-certification");
    }

    @Test
    void certificationPassesWhenRealtimePipelineAndJobProofPass() {
        CdpWarehouseProductionReadinessProofService readiness = mock(CdpWarehouseProductionReadinessProofService.class);
        CdpWarehouseTableGovernanceService tableGovernance = mock(CdpWarehouseTableGovernanceService.class);
        CdpWarehouseRealtimePipelineService pipelineService = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseRealtimeJobControlService jobService = mock(CdpWarehouseRealtimeJobControlService.class);
        JdbcTemplate doris = mock(JdbcTemplate.class);
        CdpWarehouseProductionReadinessProofService.ProductionReadinessProof proof = readinessProof("PASS");
        CdpWarehouseTableGovernanceService.InspectionSummary liveInspection = inspectionSummary("PASS", 2, 2, 0, 0);
        CdpWarehouseRealtimePipelineService.PipelineStatusSummary pipelineStatus =
                new CdpWarehouseRealtimePipelineService.PipelineStatusSummary(9L, 1, 1, 0, 0, List.of());
        CdpWarehouseRealtimeJobControlService.JobStatusSummary jobStatus =
                new CdpWarehouseRealtimeJobControlService.JobStatusSummary(9L, 1, 1, 0, 0, List.of());
        when(readiness.proof(9L, FROM, TO, "HYBRID", List.of("audience_12")))
                .thenReturn(proof);
        when(doris.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(tableGovernance.inspectLiveAll(9L, "warehouse-e2e-certification")).thenReturn(liveInspection);
        when(pipelineService.status(9L, 5)).thenReturn(pipelineStatus);
        when(jobService.status(9L, null, 300, 100)).thenReturn(jobStatus);
        CdpWarehousePhysicalE2eCertificationService service =
                new CdpWarehousePhysicalE2eCertificationService(
                        readiness,
                        dorisProvider(doris),
                        tableGovernance,
                        provider(pipelineService),
                        provider(jobService));

        CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification =
                service.certify(9L, FROM, TO, "HYBRID", List.of("audience_12"), true, true);

        assertThat(certification.status()).isEqualTo("PASS");
        assertThat(certification.requireRealtime()).isTrue();
        assertThat(certification.realtimePipelineStatus()).isSameAs(pipelineStatus);
        assertThat(certification.realtimeJobStatus()).isSameAs(jobStatus);
        assertThat(certification.evidence())
                .extracting(CdpWarehousePhysicalE2eCertificationService.CertificationEvidence::key)
                .containsExactly(
                        "production_readiness",
                        "doris_jdbc_connectivity",
                        "live_table_contracts",
                        "realtime_pipeline_status",
                        "realtime_job_status");
    }

    @Test
    void certificationPassesWhenRealtimePipelineJobAndDataPathProofPass() {
        CdpWarehouseProductionReadinessProofService readiness = mock(CdpWarehouseProductionReadinessProofService.class);
        CdpWarehouseTableGovernanceService tableGovernance = mock(CdpWarehouseTableGovernanceService.class);
        CdpWarehouseRealtimePipelineService pipelineService = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseRealtimeJobControlService jobService = mock(CdpWarehouseRealtimeJobControlService.class);
        CdpWarehouseSyntheticDataPathProbeService dataPathProbeService =
                mock(CdpWarehouseSyntheticDataPathProbeService.class);
        JdbcTemplate doris = mock(JdbcTemplate.class);
        CdpWarehouseProductionReadinessProofService.ProductionReadinessProof proof = readinessProof("PASS");
        CdpWarehouseTableGovernanceService.InspectionSummary liveInspection = inspectionSummary("PASS", 2, 2, 0, 0);
        CdpWarehouseRealtimePipelineService.PipelineStatusSummary pipelineStatus =
                new CdpWarehouseRealtimePipelineService.PipelineStatusSummary(9L, 1, 1, 0, 0, List.of());
        CdpWarehouseRealtimeJobControlService.JobStatusSummary jobStatus =
                new CdpWarehouseRealtimeJobControlService.JobStatusSummary(9L, 1, 1, 0, 0, List.of());
        CdpWarehouseSyntheticDataPathProbeService.ProbeRunView dataPathProof =
                dataPathProof("PASS", "PASS", "PASS", 1L);
        when(readiness.proof(9L, FROM, TO, "HYBRID", List.of("audience_12")))
                .thenReturn(proof);
        when(doris.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(tableGovernance.inspectLiveAll(9L, "warehouse-e2e-certification")).thenReturn(liveInspection);
        when(pipelineService.status(9L, 5)).thenReturn(pipelineStatus);
        when(jobService.status(9L, null, 300, 100)).thenReturn(jobStatus);
        when(dataPathProbeService.run(eq(9L), any(CdpWarehouseSyntheticDataPathProbeService.RunCommand.class)))
                .thenReturn(dataPathProof);
        CdpWarehousePhysicalE2eCertificationService service =
                new CdpWarehousePhysicalE2eCertificationService(
                        readiness,
                        dorisProvider(doris),
                        tableGovernance,
                        provider(pipelineService),
                        provider(jobService),
                        provider(dataPathProbeService));

        CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification =
                service.certify(9L, FROM, TO, "HYBRID", List.of("audience_12"), true, true, true);

        assertThat(certification.status()).isEqualTo("PASS");
        assertThat(certification.requireRealtime()).isTrue();
        assertThat(certification.requireDataPathProof()).isTrue();
        assertThat(certification.dataPathProof()).isSameAs(dataPathProof);
        assertThat(certification.evidence())
                .extracting(CdpWarehousePhysicalE2eCertificationService.CertificationEvidence::key)
                .containsExactly(
                        "production_readiness",
                        "doris_jdbc_connectivity",
                        "live_table_contracts",
                        "realtime_pipeline_status",
                        "realtime_job_status",
                        "synthetic_ods_data_path");
        verify(dataPathProbeService).run(eq(9L), argThat(command ->
                "e2e-certification".equals(command.probeKey())
                        && command.eventCode() == null
                        && Boolean.TRUE.equals(command.strict())
                        && Integer.valueOf(3).equals(command.verifyAttempts())
                        && Integer.valueOf(100).equals(command.verifyDelayMs())));
    }

    @Test
    void certificationFailsWhenDataPathProofServiceIsMissingAndRequired() {
        CdpWarehouseProductionReadinessProofService readiness = mock(CdpWarehouseProductionReadinessProofService.class);
        CdpWarehouseTableGovernanceService tableGovernance = mock(CdpWarehouseTableGovernanceService.class);
        JdbcTemplate doris = mock(JdbcTemplate.class);
        when(readiness.proof(9L, FROM, TO, "HYBRID", List.of()))
                .thenReturn(readinessProof("PASS"));
        when(doris.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(tableGovernance.inspectLiveAll(9L, "warehouse-e2e-certification"))
                .thenReturn(inspectionSummary("PASS", 1, 1, 0, 0));
        CdpWarehousePhysicalE2eCertificationService service =
                new CdpWarehousePhysicalE2eCertificationService(readiness, dorisProvider(doris), tableGovernance);

        CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification =
                service.certify(9L, FROM, TO, "HYBRID", List.of(), true, false, true);

        assertThat(certification.status()).isEqualTo("FAIL");
        assertThat(certification.requireDataPathProof()).isTrue();
        assertThat(certification.dataPathProof()).isNull();
        assertThat(certification.evidence()).filteredOn(row -> row.key().equals("synthetic_ods_data_path"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("FAIL");
                    assertThat(row.reason()).contains("not configured");
                });
    }

    @Test
    void certificationFailsWhenDataPathProofDoesNotPass() {
        CdpWarehouseProductionReadinessProofService readiness = mock(CdpWarehouseProductionReadinessProofService.class);
        CdpWarehouseTableGovernanceService tableGovernance = mock(CdpWarehouseTableGovernanceService.class);
        CdpWarehouseSyntheticDataPathProbeService dataPathProbeService =
                mock(CdpWarehouseSyntheticDataPathProbeService.class);
        JdbcTemplate doris = mock(JdbcTemplate.class);
        when(readiness.proof(9L, FROM, TO, "HYBRID", List.of()))
                .thenReturn(readinessProof("PASS"));
        when(doris.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(tableGovernance.inspectLiveAll(9L, "warehouse-e2e-certification"))
                .thenReturn(inspectionSummary("PASS", 1, 1, 0, 0));
        when(dataPathProbeService.run(eq(9L), any(CdpWarehouseSyntheticDataPathProbeService.RunCommand.class)))
                .thenReturn(dataPathProof("WARN", "PASS", "WARN", 0L));
        CdpWarehousePhysicalE2eCertificationService service =
                new CdpWarehousePhysicalE2eCertificationService(
                        readiness,
                        dorisProvider(doris),
                        tableGovernance,
                        null,
                        null,
                        provider(dataPathProbeService));

        CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification =
                service.certify(9L, FROM, TO, "HYBRID", List.of(), true, false, true);

        assertThat(certification.status()).isEqualTo("FAIL");
        assertThat(certification.dataPathProof().status()).isEqualTo("WARN");
        assertThat(certification.evidence()).filteredOn(row -> row.key().equals("synthetic_ods_data_path"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("FAIL");
                    assertThat(row.reason()).contains("proofStatus=WARN");
                });
    }

    @Test
    void certificationFailsWhenRealtimeProofIsRequiredAndMissing() {
        CdpWarehouseProductionReadinessProofService readiness = mock(CdpWarehouseProductionReadinessProofService.class);
        CdpWarehouseTableGovernanceService tableGovernance = mock(CdpWarehouseTableGovernanceService.class);
        CdpWarehouseRealtimePipelineService pipelineService = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseRealtimeJobControlService jobService = mock(CdpWarehouseRealtimeJobControlService.class);
        JdbcTemplate doris = mock(JdbcTemplate.class);
        when(readiness.proof(9L, FROM, TO, "HYBRID", List.of()))
                .thenReturn(readinessProof("PASS"));
        when(doris.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(tableGovernance.inspectLiveAll(9L, "warehouse-e2e-certification"))
                .thenReturn(inspectionSummary("PASS", 1, 1, 0, 0));
        when(pipelineService.status(9L, 5))
                .thenReturn(new CdpWarehouseRealtimePipelineService.PipelineStatusSummary(9L, 0, 0, 0, 0, List.of()));
        when(jobService.status(9L, null, 300, 100))
                .thenReturn(new CdpWarehouseRealtimeJobControlService.JobStatusSummary(9L, 0, 0, 0, 0, List.of()));
        CdpWarehousePhysicalE2eCertificationService service =
                new CdpWarehousePhysicalE2eCertificationService(
                        readiness,
                        dorisProvider(doris),
                        tableGovernance,
                        provider(pipelineService),
                        provider(jobService));

        CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification =
                service.certify(9L, FROM, TO, "HYBRID", List.of(), true, true);

        assertThat(certification.status()).isEqualTo("FAIL");
        assertThat(certification.evidence()).filteredOn(row -> row.key().equals("realtime_pipeline_status"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("FAIL");
                    assertThat(row.reason()).contains("no active realtime pipelines");
                });
        assertThat(certification.evidence()).filteredOn(row -> row.key().equals("realtime_job_status"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("FAIL");
                    assertThat(row.reason()).contains("no realtime jobs");
                });
    }

    @Test
    void certificationWarnsWhenRealtimeServicesAreMissingInDryRunMode() {
        CdpWarehouseProductionReadinessProofService readiness = mock(CdpWarehouseProductionReadinessProofService.class);
        CdpWarehouseTableGovernanceService tableGovernance = mock(CdpWarehouseTableGovernanceService.class);
        JdbcTemplate doris = mock(JdbcTemplate.class);
        when(readiness.proof(9L, FROM, TO, "HYBRID", List.of()))
                .thenReturn(readinessProof("PASS"));
        when(doris.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(tableGovernance.inspectLiveAll(9L, "warehouse-e2e-certification"))
                .thenReturn(inspectionSummary("PASS", 1, 1, 0, 0));
        CdpWarehousePhysicalE2eCertificationService service =
                new CdpWarehousePhysicalE2eCertificationService(
                        readiness,
                        dorisProvider(doris),
                        tableGovernance,
                        null,
                        null);

        CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification =
                service.certify(9L, FROM, TO, "HYBRID", List.of(), true, false);

        assertThat(certification.status()).isEqualTo("WARN");
        assertThat(certification.requireRealtime()).isFalse();
        assertThat(certification.evidence()).filteredOn(row -> row.key().equals("realtime_pipeline_status"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("WARN");
                    assertThat(row.reason()).contains("not configured");
                });
    }

    @Test
    void certificationFailsClosedWhenDorisIsMissingAndPhysicalProofIsRequired() {
        CdpWarehouseProductionReadinessProofService readiness = mock(CdpWarehouseProductionReadinessProofService.class);
        CdpWarehouseTableGovernanceService tableGovernance = mock(CdpWarehouseTableGovernanceService.class);
        when(readiness.proof(9L, FROM, TO, "HYBRID", List.of("audience_12")))
                .thenReturn(readinessProof("PASS"));
        CdpWarehousePhysicalE2eCertificationService service =
                new CdpWarehousePhysicalE2eCertificationService(readiness, dorisProvider(null), tableGovernance);

        CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification =
                service.certify(9L, FROM, TO, "HYBRID", List.of("audience_12"), true);

        assertThat(certification.status()).isEqualTo("FAIL");
        assertThat(certification.evidence()).filteredOn(row -> row.key().equals("doris_jdbc_connectivity"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("FAIL");
                    assertThat(row.reason()).contains("not configured");
                });
        assertThat(certification.evidence()).filteredOn(row -> row.key().equals("live_table_contracts"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("FAIL");
                    assertThat(row.reason()).contains("Doris connectivity");
                });
        verifyNoInteractions(tableGovernance);
    }

    @Test
    void certificationWarnsWhenDorisIsMissingInDryRunMode() {
        CdpWarehouseProductionReadinessProofService readiness = mock(CdpWarehouseProductionReadinessProofService.class);
        CdpWarehouseTableGovernanceService tableGovernance = mock(CdpWarehouseTableGovernanceService.class);
        when(readiness.proof(9L, FROM, TO, "HYBRID", List.of()))
                .thenReturn(readinessProof("PASS"));
        CdpWarehousePhysicalE2eCertificationService service =
                new CdpWarehousePhysicalE2eCertificationService(readiness, dorisProvider(null), tableGovernance);

        CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification =
                service.certify(9L, FROM, TO, "HYBRID", List.of(), false);

        assertThat(certification.status()).isEqualTo("WARN");
        assertThat(certification.requirePhysical()).isFalse();
        assertThat(certification.evidence()).filteredOn(row -> row.key().equals("doris_jdbc_connectivity"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("WARN");
                    assertThat(row.reason()).contains("not configured");
                });
        verifyNoInteractions(tableGovernance);
    }

    @Test
    void certificationFailsWhenLiveTableInspectionFails() {
        CdpWarehouseProductionReadinessProofService readiness = mock(CdpWarehouseProductionReadinessProofService.class);
        CdpWarehouseTableGovernanceService tableGovernance = mock(CdpWarehouseTableGovernanceService.class);
        JdbcTemplate doris = mock(JdbcTemplate.class);
        CdpWarehouseTableGovernanceService.InspectionSummary liveInspection =
                inspectionSummary("FAIL", 2, 1, 0, 1);
        when(readiness.proof(9L, FROM, TO, "HYBRID", List.of("audience_12")))
                .thenReturn(readinessProof("PASS"));
        when(doris.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(tableGovernance.inspectLiveAll(9L, "warehouse-e2e-certification")).thenReturn(liveInspection);
        CdpWarehousePhysicalE2eCertificationService service =
                new CdpWarehousePhysicalE2eCertificationService(readiness, dorisProvider(doris), tableGovernance);

        CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification certification =
                service.certify(9L, FROM, TO, "HYBRID", List.of("audience_12"), true);

        assertThat(certification.status()).isEqualTo("FAIL");
        assertThat(certification.evidence()).filteredOn(row -> row.key().equals("live_table_contracts"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.status()).isEqualTo("FAIL");
                    assertThat(row.reason()).contains("failed=1");
                });
    }

    private CdpWarehouseProductionReadinessProofService.ProductionReadinessProof readinessProof(String status) {
        return new CdpWarehouseProductionReadinessProofService.ProductionReadinessProof(
                9L,
                status,
                TO.plusMinutes(1),
                FROM,
                TO,
                "HYBRID",
                List.of(new CdpWarehouseProductionReadinessProofService.ProofEvidence(
                        "warehouse_readiness", status, "ok")),
                null,
                null,
                List.of(),
                null);
    }

    private CdpWarehouseTableGovernanceService.InspectionSummary inspectionSummary(
            String status,
            int total,
            long passed,
            long warned,
            long failed) {
        List<CdpWarehouseTableGovernanceService.InspectionReport> reports = total == 0
                ? List.of()
                : List.of(new CdpWarehouseTableGovernanceService.InspectionReport(
                        1L,
                        9L,
                        "cdp_user_event_fact",
                        "canvas_dwd.cdp_user_event_fact",
                        status,
                        8,
                        "FAIL".equals(status) ? 1 : 0,
                        "FAIL".equals(status) ? List.of("missing partition") : List.of(),
                        status,
                        "LIVE:SHOW_CREATE_TABLE",
                        TO));
        return new CdpWarehouseTableGovernanceService.InspectionSummary(
                9L,
                total,
                passed,
                warned,
                failed,
                reports);
    }

    private CdpWarehouseSyntheticDataPathProbeService.ProbeRunView dataPathProof(String status,
                                                                                 String sinkStatus,
                                                                                 String odsStatus,
                                                                                 long odsRows) {
        return new CdpWarehouseSyntheticDataPathProbeService.ProbeRunView(
                1L,
                9L,
                "e2e-certification",
                "DIRECT_SINK",
                "warehouse-probe-1",
                "__warehouse_probe__",
                "__warehouse_probe_user_1",
                true,
                status,
                status,
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
    private ObjectProvider<JdbcTemplate> dorisProvider(JdbcTemplate jdbcTemplate) {
        ObjectProvider<JdbcTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(jdbcTemplate);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
