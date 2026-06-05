package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseIncidentDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseIncidentMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseIncidentServiceTest {

    @Test
    void warningQualityCheckOpensIncident() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        service.recordQualityIncident(check("ODS_COUNT", "WARN"));

        ArgumentCaptor<CdpWarehouseIncidentDO> row = ArgumentCaptor.forClass(CdpWarehouseIncidentDO.class);
        verify(mapper).upsertOpen(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(9L);
        assertThat(row.getValue().getIncidentKey()).isEqualTo("QUALITY:ODS_COUNT");
        assertThat(row.getValue().getSeverity()).isEqualTo("WARN");
        assertThat(row.getValue().getStatus()).isEqualTo("OPEN");
        assertThat(row.getValue().getTitle()).contains("ODS_COUNT");
    }

    @Test
    void passAndSkippedQualityChecksDoNotOpenIncidents() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        service.recordQualityIncident(check("ODS_COUNT", "PASS"));
        service.recordQualityIncident(check("ODS_COUNT", "SKIPPED"));

        verify(mapper, never()).upsertOpen(any());
    }

    @Test
    void realtimePipelineWarningOpensIncident() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        service.recordRealtimePipelineIncident(pipelineIncident("WARN"));

        ArgumentCaptor<CdpWarehouseIncidentDO> row = ArgumentCaptor.forClass(CdpWarehouseIncidentDO.class);
        verify(mapper).upsertOpen(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(9L);
        assertThat(row.getValue().getIncidentKey())
                .isEqualTo("REALTIME_PIPELINE:MYSQL_CDP_EVENT_LOG_TO_DORIS_ODS");
        assertThat(row.getValue().getSourceType()).isEqualTo("WAREHOUSE_REALTIME_PIPELINE");
        assertThat(row.getValue().getSourceId()).isEqualTo(31L);
        assertThat(row.getValue().getSeverity()).isEqualTo("WARN");
        assertThat(row.getValue().getTitle()).contains("MYSQL_CDP_EVENT_LOG_TO_DORIS_ODS");
        assertThat(row.getValue().getDescription())
                .contains("checkpointId=chk-1")
                .contains("lagMs=900000")
                .contains("exceeds maxLagMs");
    }

    @Test
    void realtimePipelineFailureUsesCriticalSeverityAndPassIsIgnored() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        service.recordRealtimePipelineIncident(pipelineIncident("PASS"));
        service.recordRealtimePipelineIncident(pipelineIncident("FAIL"));

        ArgumentCaptor<CdpWarehouseIncidentDO> row = ArgumentCaptor.forClass(CdpWarehouseIncidentDO.class);
        verify(mapper).upsertOpen(row.capture());
        assertThat(row.getValue().getSeverity()).isEqualTo("CRITICAL");
        assertThat(row.getValue().getDescription()).contains("status=FAIL");
    }

    @Test
    void realtimeJobWarningOpensIncident() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        service.recordRealtimeJobIncident(jobIncident("WARN"));

        ArgumentCaptor<CdpWarehouseIncidentDO> row = ArgumentCaptor.forClass(CdpWarehouseIncidentDO.class);
        verify(mapper).upsertOpen(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(9L);
        assertThat(row.getValue().getIncidentKey()).isEqualTo("REALTIME_JOB:PIPE:JOB-A");
        assertThat(row.getValue().getSourceType()).isEqualTo("WAREHOUSE_REALTIME_JOB");
        assertThat(row.getValue().getSourceId()).isEqualTo(41L);
        assertThat(row.getValue().getSeverity()).isEqualTo("WARN");
        assertThat(row.getValue().getTitle()).contains("PIPE/JOB-A");
        assertThat(row.getValue().getDescription())
                .contains("runtimeStatus=PAUSED")
                .contains("desiredStatus=RUNNING")
                .contains("desired RUNNING");
    }

    @Test
    void realtimeJobFailureUsesCriticalSeverityAndPassIsIgnored() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        service.recordRealtimeJobIncident(jobIncident("PASS"));
        service.recordRealtimeJobIncident(jobIncident("FAIL"));

        ArgumentCaptor<CdpWarehouseIncidentDO> row = ArgumentCaptor.forClass(CdpWarehouseIncidentDO.class);
        verify(mapper).upsertOpen(row.capture());
        assertThat(row.getValue().getSeverity()).isEqualTo("CRITICAL");
        assertThat(row.getValue().getDescription())
                .contains("healthStatus=FAIL")
                .contains("lastErrorMessage=job failed");
    }

    @Test
    void readinessFailureOpensStableIncident() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        service.recordReadinessIncident(new CdpWarehouseIncidentService.ReadinessIncidentInput(
                9L,
                "offline_sync",
                "FAIL",
                "FAIL",
                "1 recent offline sync run failed",
                LocalDateTime.of(2026, 6, 5, 12, 0)));

        ArgumentCaptor<CdpWarehouseIncidentDO> row = ArgumentCaptor.forClass(CdpWarehouseIncidentDO.class);
        verify(mapper).upsertOpen(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(9L);
        assertThat(row.getValue().getIncidentKey()).isEqualTo("READINESS:OFFLINE_SYNC");
        assertThat(row.getValue().getSourceType()).isEqualTo("WAREHOUSE_READINESS");
        assertThat(row.getValue().getSourceId()).isNull();
        assertThat(row.getValue().getSeverity()).isEqualTo("CRITICAL");
        assertThat(row.getValue().getTitle()).contains("OFFLINE_SYNC");
        assertThat(row.getValue().getDescription())
                .contains("readinessStatus=FAIL")
                .contains("sectionKey=offline_sync")
                .contains("1 recent offline sync run failed");
    }

    @Test
    void passReadinessSectionDoesNotOpenIncident() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        service.recordReadinessIncident(new CdpWarehouseIncidentService.ReadinessIncidentInput(
                9L, "offline_sync", "PASS", "PASS", "ok", LocalDateTime.of(2026, 6, 5, 12, 0)));

        verify(mapper, never()).upsertOpen(any());
    }

    @Test
    void tableDriftWarningOpensStableIncident() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        service.recordTableDriftIncident(tableDriftIncident("WARN"));

        ArgumentCaptor<CdpWarehouseIncidentDO> row = ArgumentCaptor.forClass(CdpWarehouseIncidentDO.class);
        verify(mapper).upsertOpen(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(9L);
        assertThat(row.getValue().getIncidentKey()).isEqualTo("TABLE_DRIFT:CANVAS_DAILY_STATS");
        assertThat(row.getValue().getSourceType()).isEqualTo("WAREHOUSE_TABLE_DRIFT");
        assertThat(row.getValue().getSourceId()).isEqualTo(51L);
        assertThat(row.getValue().getSeverity()).isEqualTo("WARN");
        assertThat(row.getValue().getTitle()).contains("CANVAS_DAILY_STATS");
        assertThat(row.getValue().getDescription())
                .contains("physicalName=canvas_dws.canvas_daily_stats")
                .contains("source=LIVE:SHOW_CREATE_TABLE")
                .contains("violationCount=1")
                .contains("bucket count");
    }

    @Test
    void tableDriftFailureUsesCriticalSeverityAndPassIsIgnored() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        service.recordTableDriftIncident(tableDriftIncident("PASS"));
        service.recordTableDriftIncident(tableDriftIncident("FAIL"));

        ArgumentCaptor<CdpWarehouseIncidentDO> row = ArgumentCaptor.forClass(CdpWarehouseIncidentDO.class);
        verify(mapper).upsertOpen(row.capture());
        assertThat(row.getValue().getSeverity()).isEqualTo("CRITICAL");
        assertThat(row.getValue().getDescription()).contains("status=FAIL");
    }

    @Test
    void availabilityWarningOpensStableIncident() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        service.recordAvailabilityIncident(availabilityIncident("HYBRID", "offline_aggregate", "WARN"));

        ArgumentCaptor<CdpWarehouseIncidentDO> row = ArgumentCaptor.forClass(CdpWarehouseIncidentDO.class);
        verify(mapper).upsertOpen(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(9L);
        assertThat(row.getValue().getIncidentKey()).isEqualTo("AVAILABILITY:HYBRID:OFFLINE_AGGREGATE");
        assertThat(row.getValue().getSourceType()).isEqualTo("WAREHOUSE_AVAILABILITY");
        assertThat(row.getValue().getSourceId()).isNull();
        assertThat(row.getValue().getSeverity()).isEqualTo("WARN");
        assertThat(row.getValue().getTitle()).contains("HYBRID/OFFLINE_AGGREGATE");
        assertThat(row.getValue().getDescription())
                .contains("gateStatus=WARN")
                .contains("requestedTo=2026-06-05T12:00")
                .contains("availableUntil=2026-06-05T11:55");
    }

    @Test
    void availabilityFailureUsesCriticalSeverityAndPassIsIgnored() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        service.recordAvailabilityIncident(availabilityIncident("REALTIME", "realtime_pipelines", "PASS"));
        service.recordAvailabilityIncident(availabilityIncident("REALTIME", "realtime_pipelines", "FAIL"));

        ArgumentCaptor<CdpWarehouseIncidentDO> row = ArgumentCaptor.forClass(CdpWarehouseIncidentDO.class);
        verify(mapper).upsertOpen(row.capture());
        assertThat(row.getValue().getIncidentKey()).isEqualTo("AVAILABILITY:REALTIME:REALTIME_PIPELINES");
        assertThat(row.getValue().getSeverity()).isEqualTo("CRITICAL");
        assertThat(row.getValue().getDescription()).contains("decisionStatus=FAIL");
    }

    @Test
    void resolveAvailabilityIncidentUsesStableKeyAndSourceScopedUpdate() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        when(mapper.resolveAvailabilityByKey(any(), any(), any(), any())).thenReturn(1);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        assertThat(service.resolveAvailabilityIncident(9L, "hybrid", "offline_aggregate", "qa")).isTrue();

        verify(mapper).resolveAvailabilityByKey(
                org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.eq("AVAILABILITY:HYBRID:OFFLINE_AGGREGATE"),
                org.mockito.ArgumentMatchers.eq("qa"),
                any());
    }

    @Test
    void passAvailabilityDoesNotOpenIncident() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        service.recordAvailabilityIncident(availabilityIncident("HYBRID", "offline_aggregate", "PASS"));

        verify(mapper, never()).upsertOpen(any());
    }

    @Test
    void consumerAvailabilityWarningOpensStableIncident() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        service.recordConsumerAvailabilityIncident(consumerAvailabilityIncident("WARN"));

        ArgumentCaptor<CdpWarehouseIncidentDO> row = ArgumentCaptor.forClass(CdpWarehouseIncidentDO.class);
        verify(mapper).upsertOpen(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(9L);
        assertThat(row.getValue().getIncidentKey()).isEqualTo("CONSUMER_AVAILABILITY:BI_REVENUE");
        assertThat(row.getValue().getSourceType()).isEqualTo("WAREHOUSE_CONSUMER_AVAILABILITY");
        assertThat(row.getValue().getSourceId()).isNull();
        assertThat(row.getValue().getSeverity()).isEqualTo("WARN");
        assertThat(row.getValue().getTitle()).contains("BI_REVENUE");
        assertThat(row.getValue().getDescription())
                .contains("consumerType=BI_METRIC")
                .contains("consumerRef=canvas_daily_stats.success_rate")
                .contains("contractStatus=WARN")
                .contains("gatePolicy=BLOCK_ON_WARN")
                .contains("TABLE:canvas_dws.user_event_metric_daily=WARN");
    }

    @Test
    void consumerAvailabilityFailureUsesCriticalSeverityAndPassIsIgnored() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        service.recordConsumerAvailabilityIncident(consumerAvailabilityIncident("PASS"));
        service.recordConsumerAvailabilityIncident(consumerAvailabilityIncident("FAIL"));

        ArgumentCaptor<CdpWarehouseIncidentDO> row = ArgumentCaptor.forClass(CdpWarehouseIncidentDO.class);
        verify(mapper).upsertOpen(row.capture());
        assertThat(row.getValue().getIncidentKey()).isEqualTo("CONSUMER_AVAILABILITY:BI_REVENUE");
        assertThat(row.getValue().getSeverity()).isEqualTo("CRITICAL");
        assertThat(row.getValue().getDescription()).contains("contractStatus=FAIL");
    }

    @Test
    void resolveConsumerAvailabilityIncidentUsesStableKeyAndSourceScopedUpdate() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        when(mapper.resolveConsumerAvailabilityByKey(any(), any(), any(), any())).thenReturn(1);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        assertThat(service.resolveConsumerAvailabilityIncident(9L, "bi_revenue", "qa")).isTrue();

        verify(mapper).resolveConsumerAvailabilityByKey(
                org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.eq("CONSUMER_AVAILABILITY:BI_REVENUE"),
                org.mockito.ArgumentMatchers.eq("qa"),
                any());
    }

    @Test
    void listIncidentsIsTenantScopedAndBounded() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        CdpWarehouseIncidentDO row = incident(1L, "QUALITY:ODS_COUNT", "OPEN");
        when(mapper.selectList(any())).thenReturn(List.of(row));
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        List<CdpWarehouseIncidentService.IncidentView> rows = service.listIncidents(9L, "open", 200);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).incidentKey()).isEqualTo("QUALITY:ODS_COUNT");
        assertThat(rows.get(0).occurrenceCount()).isEqualTo(2);
        verify(mapper).selectList(any());
    }

    @Test
    void acknowledgeAndResolveAreTenantScoped() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        when(mapper.acknowledge(any(), any(), any(), any())).thenReturn(1);
        when(mapper.resolve(any(), any(), any(), any())).thenReturn(1);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        assertThat(service.acknowledge(9L, 1L, "operator")).isTrue();
        assertThat(service.resolve(9L, 1L, "operator")).isTrue();

        verify(mapper).acknowledge(org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("operator"),
                any());
        verify(mapper).resolve(org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("operator"),
                any());
    }

    @Test
    void resolveTableDriftIncidentUsesStableKeyAndSourceScopedUpdate() {
        CdpWarehouseIncidentMapper mapper = mock(CdpWarehouseIncidentMapper.class);
        when(mapper.resolveTableDriftByKey(any(), any(), any(), any())).thenReturn(1);
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mapper);

        assertThat(service.resolveTableDriftIncident(9L, "canvas_daily_stats", "qa")).isTrue();

        verify(mapper).resolveTableDriftByKey(
                org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.eq("TABLE_DRIFT:CANVAS_DAILY_STATS"),
                org.mockito.ArgumentMatchers.eq("qa"),
                any());
    }

    @Test
    void acknowledgeRejectsInvalidIncidentId() {
        CdpWarehouseIncidentService service = new CdpWarehouseIncidentService(mock(CdpWarehouseIncidentMapper.class));

        assertThatThrownBy(() -> service.acknowledge(9L, 0L, "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("incidentId must be positive");
    }

    private CdpWarehouseQualityService.QualityCheckResult check(String type, String status) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        return new CdpWarehouseQualityService.QualityCheckResult(
                11L, 9L, type, status, 10L, 7L, 3L,
                now.minusHours(1), now, 1L, "{\"diff\":3}", now, "quality-scheduler");
    }

    private CdpWarehouseIncidentService.RealtimePipelineIncidentInput pipelineIncident(String status) {
        return new CdpWarehouseIncidentService.RealtimePipelineIncidentInput(
                9L,
                31L,
                "mysql_cdp_event_log_to_doris_ods",
                "canvas_ods.cdp_event_log",
                status,
                "lag 900000ms exceeds maxLagMs 300000",
                "chk-1",
                LocalDateTime.of(2026, 6, 5, 12, 0),
                900_000L,
                List.of("lag 900000ms exceeds maxLagMs 300000"));
    }

    private CdpWarehouseIncidentService.RealtimeJobIncidentInput jobIncident(String status) {
        return new CdpWarehouseIncidentService.RealtimeJobIncidentInput(
                9L,
                41L,
                "pipe",
                "job-a",
                "FLINK",
                "flink-1",
                "deployment/flink-1",
                "PAUSED",
                "RUNNING",
                status,
                LocalDateTime.of(2026, 6, 5, 12, 0),
                "data-platform",
                "job failed",
                List.of("desired RUNNING but runtime is PAUSED"));
    }

    private CdpWarehouseIncidentService.TableDriftIncidentInput tableDriftIncident(String status) {
        return new CdpWarehouseIncidentService.TableDriftIncidentInput(
                9L,
                51L,
                "canvas_daily_stats",
                "canvas_dws.canvas_daily_stats",
                status,
                8,
                1,
                List.of("DDL bucket count is not 8"),
                "WARN with 1 violation(s): DDL bucket count is not 8",
                "LIVE:SHOW_CREATE_TABLE",
                LocalDateTime.of(2026, 6, 5, 12, 0));
    }

    private CdpWarehouseIncidentService.AvailabilityIncidentInput availabilityIncident(String mode,
                                                                                      String gateKey,
                                                                                      String status) {
        return new CdpWarehouseIncidentService.AvailabilityIncidentInput(
                9L,
                mode,
                status,
                gateKey,
                status,
                "requested window extends 5m past watermark",
                LocalDateTime.of(2026, 6, 5, 11, 0),
                LocalDateTime.of(2026, 6, 5, 12, 0),
                LocalDateTime.of(2026, 6, 5, 11, 55),
                5L,
                1,
                LocalDateTime.of(2026, 6, 5, 12, 0));
    }

    private CdpWarehouseIncidentService.ConsumerAvailabilityIncidentInput consumerAvailabilityIncident(String status) {
        return new CdpWarehouseIncidentService.ConsumerAvailabilityIncidentInput(
                9L,
                "bi_revenue",
                "BI_METRIC",
                "canvas_daily_stats.success_rate",
                "HYBRID",
                status,
                false,
                "BLOCK_ON_WARN",
                "consumer availability " + status + " blocked by BLOCK_ON_WARN",
                LocalDateTime.of(2026, 6, 5, 11, 0),
                LocalDateTime.of(2026, 6, 5, 12, 0),
                LocalDateTime.of(2026, 6, 5, 12, 1),
                List.of("TABLE:canvas_dws.user_event_metric_daily=" + status
                        + "(requested window extends 5m past asset availability)"));
    }

    private CdpWarehouseIncidentDO incident(Long id, String key, String status) {
        CdpWarehouseIncidentDO row = new CdpWarehouseIncidentDO();
        row.setId(id);
        row.setTenantId(9L);
        row.setIncidentKey(key);
        row.setSourceType("WAREHOUSE_QUALITY_CHECK");
        row.setSourceId(11L);
        row.setSeverity("WARN");
        row.setStatus(status);
        row.setTitle("title");
        row.setDescription("description");
        row.setOccurrenceCount(2L);
        row.setFirstSeenAt(LocalDateTime.of(2026, 6, 5, 10, 0));
        row.setLastSeenAt(LocalDateTime.of(2026, 6, 5, 12, 0));
        return row;
    }
}
