package org.chovy.canvas.domain.bi.dataset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiAuditLogDO;
import org.chovy.canvas.dal.dataobject.BiDatasetAccelerationPolicyDO;
import org.chovy.canvas.dal.dataobject.BiDatasetExtractRefreshRunDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.dal.mapper.BiDatasetAccelerationPolicyMapper;
import org.chovy.canvas.dal.mapper.BiDatasetExtractRefreshRunMapper;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.MarketingBiDatasetRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BiDatasetAccelerationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-06T10:15:30Z"), ZoneOffset.UTC);

    @Test
    void upsertsExtractPolicyAndWritesAuditSnapshot() throws Exception {
        BiDatasetAccelerationPolicyMapper policyMapper = mock(BiDatasetAccelerationPolicyMapper.class);
        BiDatasetExtractRefreshRunMapper runMapper = mock(BiDatasetExtractRefreshRunMapper.class);
        BiAuditLogMapper auditLogMapper = mock(BiAuditLogMapper.class);
        BiDatasetAccelerationPolicyDO existing = policyRow("canvas_daily_stats", false, "DIRECT_QUERY", "MANUAL");
        existing.setId(11L);
        when(policyMapper.selectList(any())).thenReturn(List.of(existing), List.of(policyRow(
                "canvas_daily_stats",
                true,
                "EXTRACT",
                "SCHEDULED")));
        when(runMapper.selectList(any())).thenReturn(List.of());
        BiDatasetAccelerationService service = service(policyMapper, runMapper, auditLogMapper, null);

        BiDatasetAccelerationPolicyView view = service.upsertPolicy(
                7L,
                "canvas_daily_stats",
                new BiDatasetAccelerationPolicyCommand(
                        true,
                        "EXTRACT",
                        "SCHEDULED",
                        30L,
                        900L,
                        500_000L,
                        "0 0/30 * * * ?"),
                "alice");

        ArgumentCaptor<BiDatasetAccelerationPolicyDO> updated = ArgumentCaptor.forClass(BiDatasetAccelerationPolicyDO.class);
        verify(policyMapper).updateById(updated.capture());
        assertThat(updated.getValue().getId()).isEqualTo(11L);
        assertThat(updated.getValue().getTenantId()).isEqualTo(7L);
        assertThat(updated.getValue().getDatasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(updated.getValue().getEnabled()).isTrue();
        assertThat(updated.getValue().getAccelerationMode()).isEqualTo("EXTRACT");
        assertThat(updated.getValue().getRefreshMode()).isEqualTo("SCHEDULED");
        assertThat(updated.getValue().getRefreshIntervalMinutes()).isEqualTo(30L);
        assertThat(updated.getValue().getTtlSeconds()).isEqualTo(900L);
        assertThat(updated.getValue().getMaxRows()).isEqualTo(500_000L);
        assertThat(updated.getValue().getCronExpression()).isEqualTo("0 0/30 * * * ?");
        assertThat(updated.getValue().getUpdatedBy()).isEqualTo("alice");
        assertThat(view.accelerationMode()).isEqualTo("EXTRACT");

        ArgumentCaptor<BiAuditLogDO> audit = ArgumentCaptor.forClass(BiAuditLogDO.class);
        verify(auditLogMapper).insert(audit.capture());
        assertThat(audit.getValue().getTenantId()).isEqualTo(7L);
        assertThat(audit.getValue().getActorId()).isEqualTo("alice");
        assertThat(audit.getValue().getActionKey()).isEqualTo("BI_DATASET_ACCELERATION_POLICY_UPDATE");
        JsonNode detail = new ObjectMapper().readTree(audit.getValue().getDetailJson());
        assertThat(detail.path("before").path("accelerationMode").asText()).isEqualTo("DIRECT_QUERY");
        assertThat(detail.path("after").path("accelerationMode").asText()).isEqualTo("EXTRACT");
    }

    @Test
    void refreshNowMaterializesExtractDatasetAndUpdatesRunAndPolicyStatus() {
        BiDatasetAccelerationPolicyMapper policyMapper = mock(BiDatasetAccelerationPolicyMapper.class);
        BiDatasetExtractRefreshRunMapper runMapper = mock(BiDatasetExtractRefreshRunMapper.class);
        BiDatasetAccelerationPolicyDO policy = policyRow("canvas_daily_stats", true, "EXTRACT", "MANUAL");
        policy.setId(21L);
        when(policyMapper.selectList(any())).thenReturn(List.of(policy));
        when(runMapper.selectList(any())).thenReturn(List.of());
        BiDatasetExtractMaterializer materializer = mock(BiDatasetExtractMaterializer.class);
        when(materializer.materialize(any(), any(), any())).thenReturn(new BiDatasetExtractMaterializationResult(
                "bi_extract.t7_canvas_daily_stats_20260606101530",
                42_000L,
                137L));
        BiDatasetAccelerationService service = service(policyMapper, runMapper, mock(BiAuditLogMapper.class), materializer);

        BiDatasetExtractRefreshRunView run = service.refreshNow(7L, "canvas_daily_stats", "alice");

        ArgumentCaptor<BiDatasetExtractRefreshRunDO> insertedRun = ArgumentCaptor.forClass(BiDatasetExtractRefreshRunDO.class);
        ArgumentCaptor<BiDatasetExtractRefreshRunDO> updatedRun = ArgumentCaptor.forClass(BiDatasetExtractRefreshRunDO.class);
        verify(runMapper).insert(insertedRun.capture());
        verify(runMapper).updateById(updatedRun.capture());
        assertThat(insertedRun.getValue().getTenantId()).isEqualTo(7L);
        assertThat(insertedRun.getValue().getDatasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(insertedRun.getValue().getStatus()).isEqualTo("RUNNING");
        assertThat(insertedRun.getValue().getRequestedBy()).isEqualTo("alice");
        assertThat(updatedRun.getValue().getStatus()).isEqualTo("SUCCESS");
        assertThat(updatedRun.getValue().getMaterializedTable()).isEqualTo("bi_extract.t7_canvas_daily_stats_20260606101530");
        assertThat(updatedRun.getValue().getRowCount()).isEqualTo(42_000L);
        assertThat(updatedRun.getValue().getDurationMs()).isEqualTo(137L);

        ArgumentCaptor<BiDatasetAccelerationPolicyDO> updatedPolicy = ArgumentCaptor.forClass(BiDatasetAccelerationPolicyDO.class);
        verify(policyMapper).updateById(updatedPolicy.capture());
        assertThat(updatedPolicy.getValue().getLastStatus()).isEqualTo("SUCCESS");
        assertThat(updatedPolicy.getValue().getMaterializedTable()).isEqualTo("bi_extract.t7_canvas_daily_stats_20260606101530");
        assertThat(updatedPolicy.getValue().getLastRefreshedAt()).isEqualTo(LocalDateTime.of(2026, 6, 6, 10, 15, 30));
        assertThat(run.status()).isEqualTo("SUCCESS");
        assertThat(run.rowCount()).isEqualTo(42_000L);
    }

    @Test
    void refreshNowDropsStaleExtractTablesBeyondRetentionLimit() {
        BiDatasetAccelerationPolicyMapper policyMapper = mock(BiDatasetAccelerationPolicyMapper.class);
        BiDatasetExtractRefreshRunMapper runMapper = mock(BiDatasetExtractRefreshRunMapper.class);
        BiDatasetAccelerationPolicyDO policy = policyRow("canvas_daily_stats", true, "EXTRACT", "MANUAL");
        policy.setId(21L);
        when(policyMapper.selectList(any())).thenReturn(List.of(policy));
        when(runMapper.selectList(any())).thenReturn(
                List.of(),
                List.of(
                        successRun(91L, "bi_extract.t7_canvas_daily_stats_20260606101530", 42_000L,
                                LocalDateTime.of(2026, 6, 6, 10, 15, 30), "ACTIVE"),
                        successRun(90L, "bi_extract.t7_canvas_daily_stats_20260606091530", 41_500L,
                                LocalDateTime.of(2026, 6, 6, 9, 15, 30), "ACTIVE"),
                        successRun(89L, "bi_extract.t7_canvas_daily_stats_20260606081530", 41_000L,
                                LocalDateTime.of(2026, 6, 6, 8, 15, 30), "ACTIVE")));
        BiDatasetExtractMaterializer materializer = mock(BiDatasetExtractMaterializer.class);
        when(materializer.materialize(any(), any(), any())).thenReturn(new BiDatasetExtractMaterializationResult(
                "bi_extract.t7_canvas_daily_stats_20260606101530",
                42_000L,
                137L));
        when(materializer.dropMaterializedTable("bi_extract.t7_canvas_daily_stats_20260606081530")).thenReturn(true);
        BiDatasetAccelerationService service = service(
                policyMapper,
                runMapper,
                mock(BiAuditLogMapper.class),
                materializer,
                2);

        service.refreshNow(7L, "canvas_daily_stats", "alice");

        verify(materializer).dropMaterializedTable("bi_extract.t7_canvas_daily_stats_20260606081530");
        ArgumentCaptor<BiDatasetExtractRefreshRunDO> updatedRuns =
                ArgumentCaptor.forClass(BiDatasetExtractRefreshRunDO.class);
        verify(runMapper, times(2)).updateById(updatedRuns.capture());
        assertThat(updatedRuns.getAllValues()).anySatisfy(row -> {
            assertThat(row.getId()).isEqualTo(89L);
            assertThat(row.getRetentionStatus()).isEqualTo("DROPPED");
            assertThat(row.getDroppedAt()).isEqualTo(LocalDateTime.of(2026, 6, 6, 10, 15, 30));
        });
    }

    @Test
    void summarizesExtractCapacityFromRefreshRuns() {
        BiDatasetAccelerationPolicyMapper policyMapper = mock(BiDatasetAccelerationPolicyMapper.class);
        BiDatasetExtractRefreshRunMapper runMapper = mock(BiDatasetExtractRefreshRunMapper.class);
        BiDatasetAccelerationPolicyDO policy = policyRow("canvas_daily_stats", true, "EXTRACT", "SCHEDULED");
        policy.setMaterializedTable("bi_extract.t7_canvas_daily_stats_20260606101530");
        policy.setLastStatus("SUCCESS");
        policy.setLastRefreshedAt(LocalDateTime.of(2026, 6, 6, 10, 15, 30));
        when(policyMapper.selectList(any())).thenReturn(List.of(policy));
        when(runMapper.selectList(any())).thenReturn(List.of(
                successRun(91L, "bi_extract.t7_canvas_daily_stats_20260606101530", 42_000L,
                        LocalDateTime.of(2026, 6, 6, 10, 15, 30), "ACTIVE"),
                successRun(90L, "bi_extract.t7_canvas_daily_stats_20260606091530", 41_500L,
                        LocalDateTime.of(2026, 6, 6, 9, 15, 30), "ACTIVE"),
                successRun(89L, "bi_extract.t7_canvas_daily_stats_20260606081530", 41_000L,
                        LocalDateTime.of(2026, 6, 6, 8, 15, 30), "DROPPED"),
                failedRun(88L, LocalDateTime.of(2026, 6, 6, 7, 15, 30))));
        BiDatasetAccelerationService service = service(policyMapper, runMapper, mock(BiAuditLogMapper.class), null, 2);

        BiDatasetExtractCapacitySummaryView summary =
                service.capacitySummary(7L, "canvas_daily_stats", 20);

        assertThat(summary.datasetKey()).isEqualTo("canvas_daily_stats");
        assertThat(summary.accelerationMode()).isEqualTo("EXTRACT");
        assertThat(summary.materializedTable()).isEqualTo("bi_extract.t7_canvas_daily_stats_20260606101530");
        assertThat(summary.successfulRuns()).isEqualTo(3);
        assertThat(summary.failedRuns()).isEqualTo(1);
        assertThat(summary.activeTables()).isEqualTo(2);
        assertThat(summary.droppedTables()).isEqualTo(1);
        assertThat(summary.staleTables()).isZero();
        assertThat(summary.retainedRows()).isEqualTo(83_500L);
        assertThat(summary.latestRowCount()).isEqualTo(42_000L);
        assertThat(summary.latestDurationMs()).isEqualTo(137L);
    }

    @Test
    void refreshNowRejectsDatasetWhenExtractModeIsNotEnabled() {
        BiDatasetAccelerationPolicyMapper policyMapper = mock(BiDatasetAccelerationPolicyMapper.class);
        BiDatasetExtractRefreshRunMapper runMapper = mock(BiDatasetExtractRefreshRunMapper.class);
        when(policyMapper.selectList(any())).thenReturn(List.of(
                policyRow("canvas_daily_stats", true, "CACHE", "MANUAL")));
        BiDatasetAccelerationService service = service(policyMapper, runMapper, mock(BiAuditLogMapper.class), null);

        assertThatThrownBy(() -> service.refreshNow(7L, "canvas_daily_stats", "alice"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EXTRACT");
    }

    @Test
    void appliesSuccessfulExtractPolicyToQueryDatasetSpec() {
        BiDatasetAccelerationPolicyMapper policyMapper = mock(BiDatasetAccelerationPolicyMapper.class);
        BiDatasetExtractRefreshRunMapper runMapper = mock(BiDatasetExtractRefreshRunMapper.class);
        BiDatasetAccelerationPolicyDO policy = policyRow("canvas_daily_stats", true, "EXTRACT", "MANUAL");
        policy.setLastStatus("SUCCESS");
        policy.setMaterializedTable("bi_extract.t7_canvas_daily_stats");
        when(policyMapper.selectList(any())).thenReturn(List.of(policy));
        BiDatasetAccelerationService service = service(policyMapper, runMapper, mock(BiAuditLogMapper.class), null);
        BiDatasetSpec original = new BiDatasetSpec(
                "canvas_daily_stats",
                "canvas_dws.canvas_daily_stats",
                "tenant_id",
                MarketingBiDatasetRegistry.dataset("canvas_daily_stats").fields(),
                MarketingBiDatasetRegistry.dataset("canvas_daily_stats").metrics(),
                List.of(),
                Map.of("connectorType", "API", "sourceKey", "api-81"));

        BiDatasetSpec accelerated = service.applyAcceleration(
                7L,
                original);

        assertThat(accelerated.tableExpression()).isEqualTo("bi_extract.t7_canvas_daily_stats");
        assertThat(accelerated.fields()).containsKeys("stat_date", "canvas_name");
        assertThat(accelerated.metrics()).containsKeys("total_executions", "success_rate");
        assertThat(accelerated.model()).containsEntry("connectorType", "API");
    }

    private BiDatasetAccelerationService service(BiDatasetAccelerationPolicyMapper policyMapper,
                                                 BiDatasetExtractRefreshRunMapper runMapper,
                                                 BiAuditLogMapper auditLogMapper,
                                                 BiDatasetExtractMaterializer materializer) {
        return service(policyMapper, runMapper, auditLogMapper, materializer, 2);
    }

    private BiDatasetAccelerationService service(BiDatasetAccelerationPolicyMapper policyMapper,
                                                 BiDatasetExtractRefreshRunMapper runMapper,
                                                 BiAuditLogMapper auditLogMapper,
                                                 BiDatasetExtractMaterializer materializer,
                                                 int retainedTables) {
        BiDatasetSpecResolver resolver = BiDatasetSpecResolver.builtIn();
        return new BiDatasetAccelerationService(
                policyMapper,
                runMapper,
                auditLogMapper,
                new ObjectMapper(),
                resolver,
                materializer == null ? BiDatasetExtractMaterializer.unavailable() : materializer,
                CLOCK,
                retainedTables);
    }

    private BiDatasetAccelerationPolicyDO policyRow(String datasetKey,
                                                    Boolean enabled,
                                                    String accelerationMode,
                                                    String refreshMode) {
        BiDatasetAccelerationPolicyDO row = new BiDatasetAccelerationPolicyDO();
        row.setTenantId(7L);
        row.setDatasetKey(datasetKey);
        row.setEnabled(enabled);
        row.setAccelerationMode(accelerationMode);
        row.setRefreshMode(refreshMode);
        row.setRefreshIntervalMinutes(60L);
        row.setTtlSeconds(300L);
        row.setMaxRows(100_000L);
        row.setUpdatedBy("ops");
        return row;
    }

    private BiDatasetExtractRefreshRunDO successRun(Long id,
                                                    String materializedTable,
                                                    Long rowCount,
                                                    LocalDateTime finishedAt,
                                                    String retentionStatus) {
        BiDatasetExtractRefreshRunDO row = new BiDatasetExtractRefreshRunDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setDatasetKey("canvas_daily_stats");
        row.setStatus("SUCCESS");
        row.setRowCount(rowCount);
        row.setDurationMs(137L);
        row.setMaterializedTable(materializedTable);
        row.setRequestedBy("scheduler");
        row.setStartedAt(finishedAt.minusSeconds(1));
        row.setFinishedAt(finishedAt);
        row.setRetentionStatus(retentionStatus);
        return row;
    }

    private BiDatasetExtractRefreshRunDO failedRun(Long id, LocalDateTime finishedAt) {
        BiDatasetExtractRefreshRunDO row = new BiDatasetExtractRefreshRunDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setDatasetKey("canvas_daily_stats");
        row.setStatus("FAILED");
        row.setRequestedBy("scheduler");
        row.setStartedAt(finishedAt.minusSeconds(1));
        row.setFinishedAt(finishedAt);
        row.setErrorMessage("warehouse unavailable");
        return row;
    }
}
