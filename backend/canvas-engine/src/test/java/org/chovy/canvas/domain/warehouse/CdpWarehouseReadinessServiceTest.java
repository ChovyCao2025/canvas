package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.domain.analytics.AudienceMaterializationOperationsService;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealth;
import org.chovy.canvas.domain.bi.query.BiDatasourceHealthProvider;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CdpWarehouseReadinessServiceTest {

    @Test
    void readinessPassesWhenAllWarehouseSectionsAreHealthy() {
        CdpWarehouseOperationsService operations = mock(CdpWarehouseOperationsService.class);
        CdpWarehouseRealtimePipelineService pipelines = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseIncidentService incidents = mock(CdpWarehouseIncidentService.class);
        AudienceMaterializationOperationsService audiences = mock(AudienceMaterializationOperationsService.class);
        when(operations.status(9L, 20)).thenReturn(warehouseStatus(
                List.of(run("SUCCESS")), List.of(watermark())));
        when(pipelines.status(9L, 3)).thenReturn(pipelineStatus(2, 2, 0, 0));
        when(incidents.listIncidents(9L, "OPEN", 100)).thenReturn(List.of());
        when(audiences.recentRuns(9L, null, null, 20)).thenReturn(List.of(materializationRun("SUCCESS")));
        CdpWarehouseReadinessService service = service(
                operations, pipelines, incidents, audiences,
                () -> List.of(new BiDatasourceHealth("doris", "DORIS", true, "available")));

        CdpWarehouseReadinessService.ReadinessSummary summary = service.readiness(9L);

        assertThat(summary.status()).isEqualTo("PASS");
        assertThat(summary.sections()).extracting(CdpWarehouseReadinessService.ReadinessSection::status)
                .containsExactly("PASS", "PASS", "PASS", "PASS", "PASS");
        assertThat(summary.offline().watermarkCount()).isEqualTo(1);
        assertThat(summary.realtime().pipelineCount()).isEqualTo(2);
        assertThat(summary.bi().availableCount()).isEqualTo(1);
        assertThat(summary.audienceMaterialization().successCount()).isEqualTo(1);
    }

    @Test
    void readinessWarnsWhenProductionEvidenceIsMissingButNoSectionFailed() {
        CdpWarehouseOperationsService operations = mock(CdpWarehouseOperationsService.class);
        CdpWarehouseRealtimePipelineService pipelines = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseIncidentService incidents = mock(CdpWarehouseIncidentService.class);
        AudienceMaterializationOperationsService audiences = mock(AudienceMaterializationOperationsService.class);
        when(operations.status(9L, 20)).thenReturn(warehouseStatus(List.of(run("SUCCESS")), List.of(watermark())));
        when(pipelines.status(9L, 3)).thenReturn(pipelineStatus(0, 0, 0, 0));
        when(incidents.listIncidents(9L, "OPEN", 100)).thenReturn(List.of());
        when(audiences.recentRuns(9L, null, null, 20)).thenReturn(List.of());
        CdpWarehouseReadinessService service = service(
                operations, pipelines, incidents, audiences,
                () -> List.of(new BiDatasourceHealth("doris", "DORIS", true, "available")));

        CdpWarehouseReadinessService.ReadinessSummary summary = service.readiness(9L);

        assertThat(summary.status()).isEqualTo("WARN");
        assertThat(summary.realtime().status()).isEqualTo("WARN");
        assertThat(summary.audienceMaterialization().status()).isEqualTo("WARN");
    }

    @Test
    void readinessFailsWhenAnySectionHasAProductionFailure() {
        CdpWarehouseOperationsService operations = mock(CdpWarehouseOperationsService.class);
        CdpWarehouseRealtimePipelineService pipelines = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseIncidentService incidents = mock(CdpWarehouseIncidentService.class);
        AudienceMaterializationOperationsService audiences = mock(AudienceMaterializationOperationsService.class);
        when(operations.status(9L, 20)).thenReturn(warehouseStatus(List.of(run("SUCCESS")), List.of(watermark())));
        when(pipelines.status(9L, 3)).thenReturn(pipelineStatus(1, 0, 0, 1));
        when(incidents.listIncidents(9L, "OPEN", 100)).thenReturn(List.of(incident("CRITICAL")));
        when(audiences.recentRuns(9L, null, null, 20)).thenReturn(List.of(materializationRun("SUCCESS")));
        CdpWarehouseReadinessService service = service(
                operations, pipelines, incidents, audiences,
                () -> List.of(new BiDatasourceHealth("doris", "DORIS", false, "down")));

        CdpWarehouseReadinessService.ReadinessSummary summary = service.readiness(9L);

        assertThat(summary.status()).isEqualTo("FAIL");
        assertThat(summary.realtime().failedCount()).isEqualTo(1);
        assertThat(summary.incidents().criticalCount()).isEqualTo(1);
        assertThat(summary.bi().unavailableCount()).isEqualTo(1);
    }

    @Test
    void readinessFailsWhenRealtimeJobFailsEvenIfPipelinesPass() {
        CdpWarehouseOperationsService operations = mock(CdpWarehouseOperationsService.class);
        CdpWarehouseRealtimePipelineService pipelines = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseRealtimeJobControlService jobs = mock(CdpWarehouseRealtimeJobControlService.class);
        CdpWarehouseIncidentService incidents = mock(CdpWarehouseIncidentService.class);
        AudienceMaterializationOperationsService audiences = mock(AudienceMaterializationOperationsService.class);
        when(operations.status(9L, 20)).thenReturn(warehouseStatus(List.of(run("SUCCESS")), List.of(watermark())));
        when(pipelines.status(9L, 3)).thenReturn(pipelineStatus(1, 1, 0, 0));
        when(jobs.status(9L, null, 300, 50)).thenReturn(jobStatus(2, 1, 0, 1));
        when(incidents.listIncidents(9L, "OPEN", 100)).thenReturn(List.of());
        when(audiences.recentRuns(9L, null, null, 20)).thenReturn(List.of(materializationRun("SUCCESS")));
        CdpWarehouseReadinessService service = service(
                operations, pipelines, jobs, incidents, audiences,
                () -> List.of(new BiDatasourceHealth("doris", "DORIS", true, "available")));

        CdpWarehouseReadinessService.ReadinessSummary summary = service.readiness(9L);

        assertThat(summary.status()).isEqualTo("FAIL");
        assertThat(summary.realtime().status()).isEqualTo("FAIL");
        assertThat(summary.realtime().pipelineCount()).isEqualTo(1);
        assertThat(summary.realtime().jobCount()).isEqualTo(2);
        assertThat(summary.realtime().failedCount()).isEqualTo(1);
        assertThat(summary.realtime().reason()).contains("pipeline/job");
    }

    @Test
    void readinessSurfacesDependencyExceptionsAsFailingSections() {
        CdpWarehouseOperationsService operations = mock(CdpWarehouseOperationsService.class);
        CdpWarehouseRealtimePipelineService pipelines = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseIncidentService incidents = mock(CdpWarehouseIncidentService.class);
        AudienceMaterializationOperationsService audiences = mock(AudienceMaterializationOperationsService.class);
        when(operations.status(9L, 20)).thenThrow(new IllegalStateException("run store unavailable"));
        when(pipelines.status(9L, 3)).thenReturn(pipelineStatus(1, 1, 0, 0));
        when(incidents.listIncidents(9L, "OPEN", 100)).thenReturn(List.of());
        when(audiences.recentRuns(9L, null, null, 20)).thenReturn(List.of(materializationRun("SUCCESS")));
        CdpWarehouseReadinessService service = service(
                operations, pipelines, incidents, audiences,
                () -> List.of(new BiDatasourceHealth("doris", "DORIS", true, "available")));

        CdpWarehouseReadinessService.ReadinessSummary summary = service.readiness(9L);

        assertThat(summary.status()).isEqualTo("FAIL");
        assertThat(summary.offline().status()).isEqualTo("FAIL");
        assertThat(summary.offline().reason()).contains("run store unavailable");
    }

    @Test
    void readinessFailsWhenOfflineEvidenceExceedsSloPolicy() {
        CdpWarehouseOperationsService operations = mock(CdpWarehouseOperationsService.class);
        CdpWarehouseRealtimePipelineService pipelines = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseIncidentService incidents = mock(CdpWarehouseIncidentService.class);
        AudienceMaterializationOperationsService audiences = mock(AudienceMaterializationOperationsService.class);
        when(operations.status(9L, 20)).thenReturn(warehouseStatus(
                List.of(staleRun("SUCCESS", 90)), List.of(staleWatermark(90))));
        when(pipelines.status(9L, 3)).thenReturn(pipelineStatus(1, 1, 0, 0));
        when(incidents.listIncidents(9L, "OPEN", 100)).thenReturn(List.of());
        when(audiences.recentRuns(9L, null, null, 20)).thenReturn(List.of(materializationRun("SUCCESS")));
        CdpWarehouseSloPolicyService sloPolicy = mock(CdpWarehouseSloPolicyService.class);
        when(sloPolicy.effectivePolicy(9L)).thenReturn(policy(10, 30, 10, 30, 1440, 4320));
        CdpWarehouseReadinessService service = service(
                operations, pipelines, incidents, audiences,
                () -> List.of(new BiDatasourceHealth("doris", "DORIS", true, "available")),
                sloPolicy);

        CdpWarehouseReadinessService.ReadinessSummary summary = service.readiness(9L);

        assertThat(summary.status()).isEqualTo("FAIL");
        assertThat(summary.offline().status()).isEqualTo("FAIL");
        assertThat(summary.offline().reason()).contains("fail threshold 30m");
    }

    @Test
    void readinessWarnsWhenAudienceMaterializationExceedsSloPolicyWarnThreshold() {
        CdpWarehouseOperationsService operations = mock(CdpWarehouseOperationsService.class);
        CdpWarehouseRealtimePipelineService pipelines = mock(CdpWarehouseRealtimePipelineService.class);
        CdpWarehouseIncidentService incidents = mock(CdpWarehouseIncidentService.class);
        AudienceMaterializationOperationsService audiences = mock(AudienceMaterializationOperationsService.class);
        when(operations.status(9L, 20)).thenReturn(warehouseStatus(List.of(run("SUCCESS")), List.of(watermark())));
        when(pipelines.status(9L, 3)).thenReturn(pipelineStatus(1, 1, 0, 0));
        when(incidents.listIncidents(9L, "OPEN", 100)).thenReturn(List.of());
        when(audiences.recentRuns(9L, null, null, 20))
                .thenReturn(List.of(staleMaterializationRun("SUCCESS", 90)));
        CdpWarehouseSloPolicyService sloPolicy = mock(CdpWarehouseSloPolicyService.class);
        when(sloPolicy.effectivePolicy(9L)).thenReturn(policy(120, 360, 120, 360, 30, 180));
        CdpWarehouseReadinessService service = service(
                operations, pipelines, incidents, audiences,
                () -> List.of(new BiDatasourceHealth("doris", "DORIS", true, "available")),
                sloPolicy);

        CdpWarehouseReadinessService.ReadinessSummary summary = service.readiness(9L);

        assertThat(summary.status()).isEqualTo("WARN");
        assertThat(summary.audienceMaterialization().status()).isEqualTo("WARN");
        assertThat(summary.audienceMaterialization().reason()).contains("warn threshold 30m");
    }

    private CdpWarehouseReadinessService service(CdpWarehouseOperationsService operations,
                                                 CdpWarehouseRealtimePipelineService pipelines,
                                                 CdpWarehouseIncidentService incidents,
                                                 AudienceMaterializationOperationsService audiences,
                                                 BiDatasourceHealthProvider bi) {
        return new CdpWarehouseReadinessService(operations, pipelines, incidents, audiences, bi);
    }

    private CdpWarehouseReadinessService service(CdpWarehouseOperationsService operations,
                                                 CdpWarehouseRealtimePipelineService pipelines,
                                                 CdpWarehouseIncidentService incidents,
                                                 AudienceMaterializationOperationsService audiences,
                                                 BiDatasourceHealthProvider bi,
                                                 CdpWarehouseSloPolicyService sloPolicy) {
        return new CdpWarehouseReadinessService(operations, pipelines, incidents, audiences, bi, sloPolicy);
    }

    private CdpWarehouseReadinessService service(CdpWarehouseOperationsService operations,
                                                 CdpWarehouseRealtimePipelineService pipelines,
                                                 CdpWarehouseRealtimeJobControlService jobs,
                                                 CdpWarehouseIncidentService incidents,
                                                 AudienceMaterializationOperationsService audiences,
                                                 BiDatasourceHealthProvider bi) {
        return new CdpWarehouseReadinessService(operations, pipelines, jobs, incidents, audiences, bi, null);
    }

    private CdpWarehouseOperationsService.WarehouseStatus warehouseStatus(
            List<CdpWarehouseOperationsService.RunRow> runs,
            List<CdpWarehouseOperationsService.WatermarkRow> watermarks) {
        return new CdpWarehouseOperationsService.WarehouseStatus(9L, runs, watermarks);
    }

    private CdpWarehouseOperationsService.RunRow run(String status) {
        LocalDateTime now = LocalDateTime.now();
        return new CdpWarehouseOperationsService.RunRow(
                1L,
                "CDP_EVENT_BACKFILL",
                "cdp_event_log",
                1L,
                10L,
                null,
                null,
                status,
                10L,
                0L,
                null,
                now.minusMinutes(2),
                now.minusMinutes(1),
                "operator");
    }

    private CdpWarehouseOperationsService.RunRow staleRun(String status, int ageMinutes) {
        LocalDateTime now = LocalDateTime.now();
        return new CdpWarehouseOperationsService.RunRow(
                1L,
                "CDP_EVENT_BACKFILL",
                "cdp_event_log",
                1L,
                10L,
                null,
                null,
                status,
                10L,
                0L,
                null,
                now.minusMinutes(ageMinutes + 1L),
                now.minusMinutes(ageMinutes),
                "operator");
    }

    private CdpWarehouseOperationsService.WatermarkRow watermark() {
        LocalDateTime now = LocalDateTime.now();
        return new CdpWarehouseOperationsService.WatermarkRow(
                1L,
                "CDP_EVENT_BACKFILL",
                "LAST_EVENT_ID",
                "10",
                now.minusMinutes(1),
                now.minusMinutes(1));
    }

    private CdpWarehouseOperationsService.WatermarkRow staleWatermark(int ageMinutes) {
        LocalDateTime now = LocalDateTime.now();
        return new CdpWarehouseOperationsService.WatermarkRow(
                1L,
                "CDP_EVENT_BACKFILL",
                "LAST_EVENT_ID",
                "10",
                now.minusMinutes(ageMinutes),
                now.minusMinutes(ageMinutes));
    }

    private CdpWarehouseRealtimePipelineService.PipelineStatusSummary pipelineStatus(
            int total,
            long passed,
            long warned,
            long failed) {
        return new CdpWarehouseRealtimePipelineService.PipelineStatusSummary(
                9L, total, passed, warned, failed, List.of());
    }

    private CdpWarehouseRealtimeJobControlService.JobStatusSummary jobStatus(
            int total,
            long passed,
            long warned,
            long failed) {
        return new CdpWarehouseRealtimeJobControlService.JobStatusSummary(
                9L, total, passed, warned, failed, List.of());
    }

    private CdpWarehouseIncidentService.IncidentView incident(String severity) {
        return new CdpWarehouseIncidentService.IncidentView(
                1L,
                9L,
                "QUALITY:ODS_COUNT",
                "WAREHOUSE_QUALITY_CHECK",
                100L,
                severity,
                "OPEN",
                "Warehouse quality FAIL",
                "details",
                1L,
                LocalDateTime.parse("2026-06-05T03:00:00"),
                LocalDateTime.parse("2026-06-05T03:01:00"),
                null,
                null,
                null,
                null);
    }

    private AudienceMaterializationOperationsService.RunView materializationRun(String status) {
        LocalDateTime now = LocalDateTime.now();
        return new AudienceMaterializationOperationsService.RunView(
                1L,
                9L,
                12L,
                2L,
                status,
                "SUCCESS".equals(status) ? 20L : 0L,
                "audience:bitmap:12:v:2",
                null,
                now.minusMinutes(2),
                now.minusMinutes(1),
                "operator");
    }

    private AudienceMaterializationOperationsService.RunView staleMaterializationRun(String status, int ageMinutes) {
        LocalDateTime now = LocalDateTime.now();
        return new AudienceMaterializationOperationsService.RunView(
                1L,
                9L,
                12L,
                2L,
                status,
                "SUCCESS".equals(status) ? 20L : 0L,
                "audience:bitmap:12:v:2",
                null,
                now.minusMinutes(ageMinutes + 1L),
                now.minusMinutes(ageMinutes),
                "operator");
    }

    private CdpWarehouseSloPolicyService.SloPolicyView policy(
            int offlineWarnRunGapMinutes,
            int offlineFailRunGapMinutes,
            int offlineWarnWatermarkLagMinutes,
            int offlineFailWatermarkLagMinutes,
            int audienceWarnRunGapMinutes,
            int audienceFailRunGapMinutes) {
        return new CdpWarehouseSloPolicyService.SloPolicyView(
                1L,
                9L,
                CdpWarehouseSloPolicyService.DEFAULT_POLICY_KEY,
                "test policy",
                offlineWarnRunGapMinutes,
                offlineFailRunGapMinutes,
                offlineWarnWatermarkLagMinutes,
                offlineFailWatermarkLagMinutes,
                audienceWarnRunGapMinutes,
                audienceFailRunGapMinutes,
                "ACTIVE",
                "data-platform",
                "test");
    }
}
