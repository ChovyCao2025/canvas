package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseStreamCheckpointDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseStreamPipelineDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseStreamCheckpointMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseStreamPipelineMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseRealtimePipelineServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-05T00:00:00Z"), ZoneId.of("UTC"));

    @Test
    void upsertPipelineNormalizesAndPersistsContract() {
        CdpWarehouseStreamPipelineMapper pipelineMapper = mock(CdpWarehouseStreamPipelineMapper.class);
        CdpWarehouseRealtimePipelineService service =
                service(pipelineMapper, mock(CdpWarehouseStreamCheckpointMapper.class));

        CdpWarehouseRealtimePipelineService.PipelineContractView result =
                service.upsertPipeline(9L, new CdpWarehouseRealtimePipelineService.PipelineContractCommand(
                        "tenant_cdc", null, "mysql_cdc", "canvas.orders", "canvas.orders",
                        "cg", "flink_cdc", "doris", "canvas_ods.orders", "exactly_once",
                        null, null, null, null, "ops", "{\"parallelism\":4}"));

        assertThat(result.tenantId()).isEqualTo(9L);
        assertThat(result.displayName()).isEqualTo("tenant_cdc");
        assertThat(result.sourceType()).isEqualTo("MYSQL_CDC");
        assertThat(result.processorType()).isEqualTo("FLINK_CDC");
        assertThat(result.deliverySemantics()).isEqualTo("EXACTLY_ONCE");
        assertThat(result.checkpointIntervalSeconds()).isEqualTo(60);
        assertThat(result.maxLagMs()).isEqualTo(600_000L);
        assertThat(result.lifecycleStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<CdpWarehouseStreamPipelineDO> row =
                ArgumentCaptor.forClass(CdpWarehouseStreamPipelineDO.class);
        verify(pipelineMapper).upsert(row.capture());
        assertThat(row.getValue().getSinkRef()).isEqualTo("canvas_ods.orders");
    }

    @Test
    void upsertPipelineRejectsUnknownDeliverySemantics() {
        CdpWarehouseRealtimePipelineService service =
                service(mock(CdpWarehouseStreamPipelineMapper.class), mock(CdpWarehouseStreamCheckpointMapper.class));

        assertThatThrownBy(() -> service.upsertPipeline(9L,
                new CdpWarehouseRealtimePipelineService.PipelineContractCommand(
                        "bad", "Bad", "MYSQL_CDC", "canvas.bad", null, null,
                        "FLINK_CDC", "DORIS", "canvas_ods.bad", "MAYBE_ONCE",
                        60, 100L, 60, "ACTIVE", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deliverySemantics");
    }

    @Test
    void listPipelinesMergesBuiltInAndTenantOverrides() {
        CdpWarehouseStreamPipelineMapper pipelineMapper = mock(CdpWarehouseStreamPipelineMapper.class);
        when(pipelineMapper.selectList(any())).thenReturn(List.of(
                pipeline(1L, 0L, "mysql_cdp_event_log_to_doris_ods", "canvas_ods.cdp_event_log", 300_000L),
                pipeline(2L, 9L, "mysql_cdp_event_log_to_doris_ods", "tenant_ods.cdp_event_log", 100_000L),
                pipeline(3L, 0L, "mysql_canvas_trace_to_doris_ods", "canvas_ods.canvas_execution_trace", 300_000L)
        ));
        CdpWarehouseRealtimePipelineService service =
                service(pipelineMapper, mock(CdpWarehouseStreamCheckpointMapper.class));

        List<CdpWarehouseRealtimePipelineService.PipelineContractView> rows =
                service.listPipelines(9L, "active");

        assertThat(rows).extracting(CdpWarehouseRealtimePipelineService.PipelineContractView::pipelineKey)
                .containsExactly("mysql_cdp_event_log_to_doris_ods", "mysql_canvas_trace_to_doris_ods");
        assertThat(rows.get(0).tenantId()).isEqualTo(9L);
        assertThat(rows.get(0).sinkRef()).isEqualTo("tenant_ods.cdp_event_log");
        assertThat(rows.get(0).maxLagMs()).isEqualTo(100_000L);
    }

    @Test
    void reportCheckpointPassesExactlyOnceWithRequiredEvidence() {
        CdpWarehouseStreamPipelineMapper pipelineMapper = mock(CdpWarehouseStreamPipelineMapper.class);
        CdpWarehouseStreamCheckpointMapper checkpointMapper = mock(CdpWarehouseStreamCheckpointMapper.class);
        when(pipelineMapper.selectList(any())).thenReturn(List.of(
                pipeline(1L, 9L, "mysql_cdp_event_log_to_doris_ods", "canvas_ods.cdp_event_log", 300_000L)));
        CdpWarehouseRealtimePipelineService service = service(pipelineMapper, checkpointMapper);

        CdpWarehouseRealtimePipelineService.CheckpointReport report = service.reportCheckpoint(9L,
                checkpoint("mysql_cdp_event_log_to_doris_ods", "chk-1", "1024", "1024", 1000L, "PASS"));

        assertThat(report.status()).isEqualTo("PASS");
        assertThat(report.message()).isEqualTo("Realtime pipeline healthy");
        assertThat(report.reasons()).isEmpty();
        ArgumentCaptor<CdpWarehouseStreamCheckpointDO> row =
                ArgumentCaptor.forClass(CdpWarehouseStreamCheckpointDO.class);
        verify(checkpointMapper).upsert(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(9L);
        assertThat(row.getValue().getCheckpointId()).isEqualTo("chk-1");
        assertThat(row.getValue().getCommittedOffset()).isEqualTo("1024");
        assertThat(row.getValue().getStatus()).isEqualTo("PASS");
        verify(pipelineMapper).updateRuntime(eq(9L), eq("mysql_cdp_event_log_to_doris_ods"),
                any(CdpWarehouseStreamPipelineDO.class), any(LocalDateTime.class));
    }

    @Test
    void reportCheckpointRecordsSinkTableAssetAvailability() {
        CdpWarehouseStreamPipelineMapper pipelineMapper = mock(CdpWarehouseStreamPipelineMapper.class);
        CdpWarehouseStreamCheckpointMapper checkpointMapper = mock(CdpWarehouseStreamCheckpointMapper.class);
        CdpWarehouseConsumerAvailabilityService availabilityService =
                mock(CdpWarehouseConsumerAvailabilityService.class);
        when(pipelineMapper.selectList(any())).thenReturn(List.of(
                pipeline(1L, 9L, "mysql_cdp_event_log_to_doris_ods", "canvas_ods.cdp_event_log", 300_000L)));
        CdpWarehouseRealtimePipelineService service =
                serviceWithAvailability(pipelineMapper, checkpointMapper, availabilityService);

        service.reportCheckpoint(9L,
                checkpoint("mysql_cdp_event_log_to_doris_ods", "chk-1", "1024", "1024", 1000L, "PASS"));

        ArgumentCaptor<CdpWarehouseConsumerAvailabilityService.AssetAvailabilityCommand> captor =
                ArgumentCaptor.forClass(CdpWarehouseConsumerAvailabilityService.AssetAvailabilityCommand.class);
        verify(availabilityService).recordAssetAvailability(eq(9L), captor.capture());
        CdpWarehouseConsumerAvailabilityService.AssetAvailabilityCommand command = captor.getValue();
        assertThat(command.assetType()).isEqualTo("TABLE");
        assertThat(command.assetKey()).isEqualTo("canvas_ods.cdp_event_log");
        assertThat(command.availabilityMode()).isEqualTo("REALTIME");
        assertThat(command.windowStart()).isNull();
        assertThat(command.windowEnd()).isEqualTo(now().minusSeconds(1));
        assertThat(command.availableUntil()).isEqualTo(now().minusSeconds(1));
        assertThat(command.status()).isEqualTo("PASS");
        assertThat(command.evidenceSource()).isEqualTo("REALTIME_CHECKPOINT");
        assertThat(command.evidenceRef()).isEqualTo("checkpoint:chk-1");
        assertThat(command.reason()).isEqualTo("Realtime pipeline healthy");
        assertThat(command.observedAt()).isEqualTo(now());
    }

    @Test
    void reportCheckpointMissingWatermarkRecordsFailAssetAvailability() {
        CdpWarehouseStreamPipelineMapper pipelineMapper = mock(CdpWarehouseStreamPipelineMapper.class);
        CdpWarehouseStreamCheckpointMapper checkpointMapper = mock(CdpWarehouseStreamCheckpointMapper.class);
        CdpWarehouseConsumerAvailabilityService availabilityService =
                mock(CdpWarehouseConsumerAvailabilityService.class);
        when(pipelineMapper.selectList(any())).thenReturn(List.of(
                pipeline(1L, 9L, "mysql_cdp_event_log_to_doris_ods", "canvas_ods.cdp_event_log", 300_000L)));
        CdpWarehouseRealtimePipelineService service =
                serviceWithAvailability(pipelineMapper, checkpointMapper, availabilityService);

        CdpWarehouseRealtimePipelineService.CheckpointReport report = service.reportCheckpoint(9L,
                new CdpWarehouseRealtimePipelineService.CheckpointCommand(
                        "mysql_cdp_event_log_to_doris_ods", "chk-no-watermark", "0", "1024", "1024",
                        null, now(), 1000L, 10L, "PASS", null, "flink"));

        assertThat(report.status()).isEqualTo("PASS");
        ArgumentCaptor<CdpWarehouseConsumerAvailabilityService.AssetAvailabilityCommand> captor =
                ArgumentCaptor.forClass(CdpWarehouseConsumerAvailabilityService.AssetAvailabilityCommand.class);
        verify(availabilityService).recordAssetAvailability(eq(9L), captor.capture());
        CdpWarehouseConsumerAvailabilityService.AssetAvailabilityCommand command = captor.getValue();
        assertThat(command.status()).isEqualTo("FAIL");
        assertThat(command.availableUntil()).isEqualTo(now());
        assertThat(command.windowEnd()).isNull();
        assertThat(command.reason()).isEqualTo("checkpoint watermark is missing");
        assertThat(command.evidenceRef()).isEqualTo("checkpoint:chk-no-watermark");
    }

    @Test
    void reportCheckpointAvailabilitySideEffectFailureDoesNotRejectRuntimeEvidence() {
        CdpWarehouseStreamPipelineMapper pipelineMapper = mock(CdpWarehouseStreamPipelineMapper.class);
        CdpWarehouseStreamCheckpointMapper checkpointMapper = mock(CdpWarehouseStreamCheckpointMapper.class);
        CdpWarehouseConsumerAvailabilityService availabilityService =
                mock(CdpWarehouseConsumerAvailabilityService.class);
        doThrow(new RuntimeException("availability store unavailable"))
                .when(availabilityService).recordAssetAvailability(any(), any());
        when(pipelineMapper.selectList(any())).thenReturn(List.of(
                pipeline(1L, 9L, "mysql_cdp_event_log_to_doris_ods", "canvas_ods.cdp_event_log", 300_000L)));
        CdpWarehouseRealtimePipelineService service =
                serviceWithAvailability(pipelineMapper, checkpointMapper, availabilityService);

        CdpWarehouseRealtimePipelineService.CheckpointReport report = service.reportCheckpoint(9L,
                checkpoint("mysql_cdp_event_log_to_doris_ods", "chk-availability-fail", "1024", "1024",
                        1000L, "PASS"));

        assertThat(report.status()).isEqualTo("PASS");
        verify(checkpointMapper).upsert(any(CdpWarehouseStreamCheckpointDO.class));
        verify(availabilityService, times(1)).recordAssetAvailability(any(), any());
    }

    @Test
    void reportCheckpointWarnsWhenExactlyOnceEvidenceIsMissing() {
        CdpWarehouseStreamPipelineMapper pipelineMapper = mock(CdpWarehouseStreamPipelineMapper.class);
        CdpWarehouseStreamCheckpointMapper checkpointMapper = mock(CdpWarehouseStreamCheckpointMapper.class);
        when(pipelineMapper.selectList(any())).thenReturn(List.of(
                pipeline(1L, 0L, "mysql_cdp_event_log_to_doris_ods", "canvas_ods.cdp_event_log", 300_000L)));
        CdpWarehouseRealtimePipelineService service = service(pipelineMapper, checkpointMapper);

        CdpWarehouseRealtimePipelineService.CheckpointReport report = service.reportCheckpoint(9L,
                checkpoint("mysql_cdp_event_log_to_doris_ods", "", "1024", "", 1000L, "PASS"));

        assertThat(report.status()).isEqualTo("WARN");
        assertThat(report.reasons()).contains(
                "exactly-once checkpoint id is missing",
                "exactly-once committed offset is missing");
        verify(pipelineMapper, never()).updateRuntime(eq(0L), eq("mysql_cdp_event_log_to_doris_ods"),
                any(CdpWarehouseStreamPipelineDO.class), any(LocalDateTime.class));
    }

    @Test
    void reportCheckpointWarnsWhenEvidenceIsOnlyJobStartupSubmission() {
        CdpWarehouseStreamPipelineMapper pipelineMapper = mock(CdpWarehouseStreamPipelineMapper.class);
        CdpWarehouseStreamCheckpointMapper checkpointMapper = mock(CdpWarehouseStreamCheckpointMapper.class);
        when(pipelineMapper.selectList(any())).thenReturn(List.of(
                pipeline(1L, 9L, "mysql_cdp_event_log_to_doris_ods", "canvas_ods.cdp_event_log", 300_000L)));
        CdpWarehouseRealtimePipelineService service = service(pipelineMapper, checkpointMapper);

        CdpWarehouseRealtimePipelineService.CheckpointReport report = service.reportCheckpoint(9L,
                new CdpWarehouseRealtimePipelineService.CheckpointCommand(
                        "mysql_cdp_event_log_to_doris_ods",
                        "mysql_cdp_event_log_to_doris_ods-startup",
                        "job-startup",
                        "submitted",
                        "submitted",
                        now(),
                        now(),
                        0L,
                        0L,
                        "PASS",
                        null,
                        "canvas-flink"));

        assertThat(report.status()).isEqualTo("WARN");
        assertThat(report.message()).contains("startup submission is not runtime checkpoint evidence");
        assertThat(report.reasons()).contains("startup submission is not runtime checkpoint evidence");
    }

    @Test
    void reportCheckpointWarnsWhenLagExceedsContract() {
        CdpWarehouseStreamPipelineMapper pipelineMapper = mock(CdpWarehouseStreamPipelineMapper.class);
        when(pipelineMapper.selectList(any())).thenReturn(List.of(
                pipeline(1L, 9L, "mysql_cdp_event_log_to_doris_ods", "canvas_ods.cdp_event_log", 300_000L)));
        CdpWarehouseRealtimePipelineService service =
                service(pipelineMapper, mock(CdpWarehouseStreamCheckpointMapper.class));

        CdpWarehouseRealtimePipelineService.CheckpointReport report = service.reportCheckpoint(9L,
                checkpoint("mysql_cdp_event_log_to_doris_ods", "chk-2", "2048", "2048", 900_000L, "PASS"));

        assertThat(report.status()).isEqualTo("WARN");
        assertThat(report.message()).contains("exceeds maxLagMs");
    }

    @Test
    void reportCheckpointWarningRecordsRealtimePipelineIncidentBestEffort() {
        CdpWarehouseStreamPipelineMapper pipelineMapper = mock(CdpWarehouseStreamPipelineMapper.class);
        CdpWarehouseStreamCheckpointMapper checkpointMapper = mock(CdpWarehouseStreamCheckpointMapper.class);
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        when(pipelineMapper.selectList(any())).thenReturn(List.of(
                pipeline(1L, 9L, "mysql_cdp_event_log_to_doris_ods", "canvas_ods.cdp_event_log", 300_000L)));
        CdpWarehouseRealtimePipelineService service = service(pipelineMapper, checkpointMapper, incidentService);

        service.reportCheckpoint(9L,
                checkpoint("mysql_cdp_event_log_to_doris_ods", "chk-2", "2048", "2048", 900_000L, "PASS"));

        verify(incidentService).recordRealtimePipelineIncident(org.mockito.ArgumentMatchers.argThat(input ->
                input.tenantId().equals(9L)
                        && input.pipelineId().equals(1L)
                        && "mysql_cdp_event_log_to_doris_ods".equals(input.pipelineKey())
                        && "WARN".equals(input.status())
                        && input.message().contains("exceeds maxLagMs")
                        && input.lagMs().equals(900_000L)));
    }

    @Test
    void reportCheckpointIncidentFailureDoesNotRejectRuntimeEvidence() {
        CdpWarehouseStreamPipelineMapper pipelineMapper = mock(CdpWarehouseStreamPipelineMapper.class);
        CdpWarehouseStreamCheckpointMapper checkpointMapper = mock(CdpWarehouseStreamCheckpointMapper.class);
        CdpWarehouseIncidentService incidentService = mock(CdpWarehouseIncidentService.class);
        doThrow(new RuntimeException("incident store unavailable"))
                .when(incidentService).recordRealtimePipelineIncident(any());
        when(pipelineMapper.selectList(any())).thenReturn(List.of(
                pipeline(1L, 9L, "mysql_cdp_event_log_to_doris_ods", "canvas_ods.cdp_event_log", 300_000L)));
        CdpWarehouseRealtimePipelineService service = service(pipelineMapper, checkpointMapper, incidentService);

        CdpWarehouseRealtimePipelineService.CheckpointReport report = service.reportCheckpoint(9L,
                checkpoint("mysql_cdp_event_log_to_doris_ods", "chk-2", "2048", "2048", 900_000L, "PASS"));

        assertThat(report.status()).isEqualTo("WARN");
        verify(checkpointMapper).upsert(any(CdpWarehouseStreamCheckpointDO.class));
    }

    @Test
    void reportCheckpointFailsWhenExternalJobReportsFailure() {
        CdpWarehouseStreamPipelineMapper pipelineMapper = mock(CdpWarehouseStreamPipelineMapper.class);
        when(pipelineMapper.selectList(any())).thenReturn(List.of(
                pipeline(1L, 9L, "mysql_cdp_event_log_to_doris_ods", "canvas_ods.cdp_event_log", 300_000L)));
        CdpWarehouseRealtimePipelineService service =
                service(pipelineMapper, mock(CdpWarehouseStreamCheckpointMapper.class));

        CdpWarehouseRealtimePipelineService.CheckpointReport report = service.reportCheckpoint(9L,
                new CdpWarehouseRealtimePipelineService.CheckpointCommand(
                        "mysql_cdp_event_log_to_doris_ods", "chk-3", "0", "4096", "4096",
                        now().minusSeconds(1), now(), 1000L, 10L, "FAIL", "flink job failed", "flink"));

        assertThat(report.status()).isEqualTo("FAIL");
        assertThat(report.message()).contains("reported status is FAIL: flink job failed");
    }

    @Test
    void reportCheckpointFailsWhenBreakingSchemaVersionIsReported() {
        CdpWarehouseStreamPipelineMapper pipelineMapper = mock(CdpWarehouseStreamPipelineMapper.class);
        CdpWarehouseStreamCheckpointMapper checkpointMapper = mock(CdpWarehouseStreamCheckpointMapper.class);
        CdpWarehouseRealtimeSchemaService schemaService = mock(CdpWarehouseRealtimeSchemaService.class);
        when(pipelineMapper.selectList(any())).thenReturn(List.of(
                pipeline(1L, 9L, "mysql_cdp_event_log_to_doris_ods", "canvas_ods.cdp_event_log", 300_000L)));
        when(schemaService.evaluateCheckpoint(9L, "mysql_cdp_event_log_to_doris_ods", "1", "2"))
                .thenReturn(new CdpWarehouseRealtimeSchemaService.SchemaCheckpointEvaluation(
                        "FAIL", List.of("sink schema version 2 is BREAKING: field removed: event_id")));
        CdpWarehouseRealtimePipelineService service = service(
                pipelineMapper, checkpointMapper, null, schemaService);

        CdpWarehouseRealtimePipelineService.CheckpointReport report = service.reportCheckpoint(9L,
                new CdpWarehouseRealtimePipelineService.CheckpointCommand(
                        "mysql_cdp_event_log_to_doris_ods", "chk-schema", "0", "4096", "4096",
                        now().minusSeconds(1), now(), 1000L, 10L, "PASS", null, "flink", "1", "2"));

        assertThat(report.status()).isEqualTo("FAIL");
        assertThat(report.schemaStatus()).isEqualTo("FAIL");
        assertThat(report.reasons()).contains("sink schema version 2 is BREAKING: field removed: event_id");
        ArgumentCaptor<CdpWarehouseStreamCheckpointDO> row =
                ArgumentCaptor.forClass(CdpWarehouseStreamCheckpointDO.class);
        verify(checkpointMapper).upsert(row.capture());
        assertThat(row.getValue().getSourceSchemaVersion()).isEqualTo("1");
        assertThat(row.getValue().getSinkSchemaVersion()).isEqualTo("2");
        assertThat(row.getValue().getSchemaStatus()).isEqualTo("FAIL");
    }

    @Test
    void reportCheckpointWarnsWhenSchemaVersionIsUnknown() {
        CdpWarehouseStreamPipelineMapper pipelineMapper = mock(CdpWarehouseStreamPipelineMapper.class);
        CdpWarehouseStreamCheckpointMapper checkpointMapper = mock(CdpWarehouseStreamCheckpointMapper.class);
        CdpWarehouseRealtimeSchemaService schemaService = mock(CdpWarehouseRealtimeSchemaService.class);
        when(pipelineMapper.selectList(any())).thenReturn(List.of(
                pipeline(1L, 9L, "mysql_cdp_event_log_to_doris_ods", "canvas_ods.cdp_event_log", 300_000L)));
        when(schemaService.evaluateCheckpoint(9L, "mysql_cdp_event_log_to_doris_ods", "missing", null))
                .thenReturn(new CdpWarehouseRealtimeSchemaService.SchemaCheckpointEvaluation(
                        "WARN", List.of("source schema version missing is not registered")));
        CdpWarehouseRealtimePipelineService service = service(
                pipelineMapper, checkpointMapper, null, schemaService);

        CdpWarehouseRealtimePipelineService.CheckpointReport report = service.reportCheckpoint(9L,
                new CdpWarehouseRealtimePipelineService.CheckpointCommand(
                        "mysql_cdp_event_log_to_doris_ods", "chk-schema-warn", "0", "4096", "4096",
                        now().minusSeconds(1), now(), 1000L, 10L, "PASS", null, "flink", "missing", null));

        assertThat(report.status()).isEqualTo("WARN");
        assertThat(report.schemaStatus()).isEqualTo("WARN");
        assertThat(report.reasons()).contains("source schema version missing is not registered");
    }

    @Test
    void statusUsesTenantCheckpointEvidenceForBuiltInContract() {
        CdpWarehouseStreamPipelineMapper pipelineMapper = mock(CdpWarehouseStreamPipelineMapper.class);
        CdpWarehouseStreamCheckpointMapper checkpointMapper = mock(CdpWarehouseStreamCheckpointMapper.class);
        when(pipelineMapper.selectList(any())).thenReturn(List.of(
                pipeline(1L, 0L, "mysql_cdp_event_log_to_doris_ods", "canvas_ods.cdp_event_log", 300_000L)));
        CdpWarehouseStreamCheckpointDO checkpoint = new CdpWarehouseStreamCheckpointDO();
        checkpoint.setId(7L);
        checkpoint.setTenantId(9L);
        checkpoint.setPipelineKey("mysql_cdp_event_log_to_doris_ods");
        checkpoint.setCheckpointId("chk-7");
        checkpoint.setSourcePartition("0");
        checkpoint.setSourceOffset("700");
        checkpoint.setCommittedOffset("700");
        checkpoint.setWatermarkTime(now().minusSeconds(2));
        checkpoint.setCheckpointTime(now().minusSeconds(1));
        checkpoint.setLagMs(1000L);
        checkpoint.setRowCount(20L);
        checkpoint.setStatus("PASS");
        checkpoint.setErrorMessage("Realtime pipeline healthy");
        checkpoint.setReportedBy("flink");
        when(checkpointMapper.selectList(any())).thenReturn(List.of(checkpoint));
        CdpWarehouseRealtimePipelineService service = service(pipelineMapper, checkpointMapper);

        CdpWarehouseRealtimePipelineService.PipelineStatusSummary status = service.status(9L, 5);

        assertThat(status.total()).isEqualTo(1);
        assertThat(status.passed()).isEqualTo(1);
        assertThat(status.pipelines().get(0).lastCheckpointId()).isEqualTo("chk-7");
        assertThat(status.pipelines().get(0).contract().tenantId()).isEqualTo(0L);
    }

    private CdpWarehouseRealtimePipelineService service(CdpWarehouseStreamPipelineMapper pipelineMapper,
                                                        CdpWarehouseStreamCheckpointMapper checkpointMapper) {
        return new CdpWarehouseRealtimePipelineService(pipelineMapper, checkpointMapper, CLOCK);
    }

    @SuppressWarnings("unchecked")
    private CdpWarehouseRealtimePipelineService service(CdpWarehouseStreamPipelineMapper pipelineMapper,
                                                        CdpWarehouseStreamCheckpointMapper checkpointMapper,
                                                        CdpWarehouseIncidentService incidentService) {
        ObjectProvider<CdpWarehouseIncidentService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(incidentService);
        return new CdpWarehouseRealtimePipelineService(pipelineMapper, checkpointMapper, CLOCK, provider);
    }

    @SuppressWarnings("unchecked")
    private CdpWarehouseRealtimePipelineService service(CdpWarehouseStreamPipelineMapper pipelineMapper,
                                                        CdpWarehouseStreamCheckpointMapper checkpointMapper,
                                                        CdpWarehouseIncidentService incidentService,
                                                        CdpWarehouseRealtimeSchemaService schemaService) {
        ObjectProvider<CdpWarehouseIncidentService> incidentProvider = mock(ObjectProvider.class);
        when(incidentProvider.getIfAvailable()).thenReturn(incidentService);
        ObjectProvider<CdpWarehouseRealtimeSchemaService> schemaProvider = mock(ObjectProvider.class);
        when(schemaProvider.getIfAvailable()).thenReturn(schemaService);
        return new CdpWarehouseRealtimePipelineService(
                pipelineMapper, checkpointMapper, CLOCK, incidentProvider, schemaProvider);
    }

    @SuppressWarnings("unchecked")
    private CdpWarehouseRealtimePipelineService serviceWithAvailability(
            CdpWarehouseStreamPipelineMapper pipelineMapper,
            CdpWarehouseStreamCheckpointMapper checkpointMapper,
            CdpWarehouseConsumerAvailabilityService availabilityService) {
        ObjectProvider<CdpWarehouseIncidentService> incidentProvider = mock(ObjectProvider.class);
        when(incidentProvider.getIfAvailable()).thenReturn(null);
        ObjectProvider<CdpWarehouseRealtimeSchemaService> schemaProvider = mock(ObjectProvider.class);
        when(schemaProvider.getIfAvailable()).thenReturn(null);
        ObjectProvider<CdpWarehouseConsumerAvailabilityService> availabilityProvider = mock(ObjectProvider.class);
        when(availabilityProvider.getIfAvailable()).thenReturn(availabilityService);
        return new CdpWarehouseRealtimePipelineService(
                pipelineMapper, checkpointMapper, CLOCK, incidentProvider, schemaProvider, availabilityProvider);
    }

    private CdpWarehouseRealtimePipelineService.CheckpointCommand checkpoint(
            String pipelineKey, String checkpointId, String sourceOffset, String committedOffset,
            Long lagMs, String status) {
        return new CdpWarehouseRealtimePipelineService.CheckpointCommand(
                pipelineKey, checkpointId, "0", sourceOffset, committedOffset,
                now().minusSeconds(1), now(), lagMs, 10L, status, null, "flink");
    }

    private CdpWarehouseStreamPipelineDO pipeline(Long id, Long tenantId, String key, String sinkRef, Long maxLagMs) {
        CdpWarehouseStreamPipelineDO row = new CdpWarehouseStreamPipelineDO();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setPipelineKey(key);
        row.setDisplayName(key);
        row.setSourceType("MYSQL_CDC");
        row.setSourceRef("canvas.cdp_event_log");
        row.setSourceTopic("canvas.cdp_event_log");
        row.setConsumerGroup("canvas-cdp-event-ods");
        row.setProcessorType("FLINK_CDC");
        row.setSinkType("DORIS");
        row.setSinkRef(sinkRef);
        row.setDeliverySemantics("EXACTLY_ONCE");
        row.setCheckpointIntervalSeconds(60);
        row.setMaxLagMs(maxLagMs);
        row.setMaxCheckpointAgeSeconds(300);
        row.setLifecycleStatus("ACTIVE");
        row.setOwnerName("data-platform");
        row.setConfigJson("{}");
        return row;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(CLOCK);
    }
}
