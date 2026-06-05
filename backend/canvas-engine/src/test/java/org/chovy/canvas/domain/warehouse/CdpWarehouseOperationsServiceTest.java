package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseSyncRunDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseWatermarkDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseSyncRunMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseWatermarkMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdpWarehouseOperationsServiceTest {

    @Test
    void statusReturnsRecentRunsAndWatermarksForTenant() {
        CdpWarehouseSyncRunMapper runMapper = mock(CdpWarehouseSyncRunMapper.class);
        CdpWarehouseWatermarkMapper watermarkMapper = mock(CdpWarehouseWatermarkMapper.class);
        CdpWarehouseSyncRunDO run = run("BACKFILL", "SUCCESS");
        CdpWarehouseWatermarkDO watermark = watermark("CDP_EVENT_BACKFILL", "LAST_EVENT_ID", "88");
        when(runMapper.selectList(any())).thenReturn(List.of(run));
        when(watermarkMapper.selectList(any())).thenReturn(List.of(watermark));
        CdpWarehouseOperationsService service = service(runMapper, watermarkMapper);

        CdpWarehouseOperationsService.WarehouseStatus status = service.status(9L, 20);

        assertThat(status.tenantId()).isEqualTo(9L);
        assertThat(status.recentRuns()).hasSize(1);
        assertThat(status.recentRuns().get(0).jobType()).isEqualTo("BACKFILL");
        assertThat(status.watermarks()).hasSize(1);
        assertThat(status.watermarks().get(0).watermarkValue()).isEqualTo("88");
    }

    @Test
    void triggerBackfillRejectsUnboundedLimit() {
        CdpWarehouseOperationsService service =
                service(mock(CdpWarehouseSyncRunMapper.class), mock(CdpWarehouseWatermarkMapper.class));

        assertThatThrownBy(() -> service.triggerBackfill(9L, 0L, 0, "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit must be between");
    }

    @Test
    void incrementalBackfillStartsFromLastEventIdWatermark() {
        CdpWarehouseWatermarkMapper watermarkMapper = mock(CdpWarehouseWatermarkMapper.class);
        when(watermarkMapper.selectOne(any()))
                .thenReturn(watermark("CDP_EVENT_BACKFILL", "LAST_EVENT_ID", "77"));
        CdpWarehouseBackfillService backfill = mock(CdpWarehouseBackfillService.class);
        when(backfill.backfill(9L, 77L, 100, "warehouse-scheduler"))
                .thenReturn(new CdpWarehouseBackfillService.BackfillResult("SUCCESS", 3, 0, 80));
        CdpWarehouseOperationsService service = service(backfill, mock(CdpWarehouseAggregationService.class),
                mock(CdpWarehouseSyncRunMapper.class), watermarkMapper);

        CdpWarehouseBackfillService.BackfillResult result = service.runIncrementalBackfill(9L, 100);

        assertThat(result.lastEventId()).isEqualTo(80);
        verify(backfill).backfill(9L, 77L, 100, "warehouse-scheduler");
    }

    @Test
    void incrementalAggregationUsesWatermarkAndCapsWindowAtNow() {
        CdpWarehouseWatermarkMapper watermarkMapper = mock(CdpWarehouseWatermarkMapper.class);
        LocalDateTime from = LocalDateTime.of(2026, 6, 5, 11, 50);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        when(watermarkMapper.selectOne(any()))
                .thenReturn(watermark("CDP_EVENT_AGGREGATE", "WINDOW_END", from.toString()));
        CdpWarehouseAggregationService aggregation = mock(CdpWarehouseAggregationService.class);
        when(aggregation.aggregate(9L, from, now, "warehouse-scheduler"))
                .thenReturn(new CdpWarehouseAggregationService.AggregationResult("SUCCESS", 2, 1));
        CdpWarehouseOperationsService service = service(mock(CdpWarehouseBackfillService.class), aggregation,
                mock(CdpWarehouseSyncRunMapper.class), watermarkMapper);

        CdpWarehouseAggregationService.AggregationResult result =
                service.runIncrementalAggregation(9L, now, 30);

        assertThat(result.status()).isEqualTo("SUCCESS");
        verify(aggregation).aggregate(9L, from, now, "warehouse-scheduler");
    }

    @Test
    void offlineCyclePlanUsesWatermarksAndMarksAggregationWaitingForBackfill() {
        CdpWarehouseWatermarkMapper watermarkMapper = mock(CdpWarehouseWatermarkMapper.class);
        LocalDateTime aggregateFrom = LocalDateTime.of(2026, 6, 5, 11, 30);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        when(watermarkMapper.selectOne(any())).thenReturn(
                watermark("CDP_EVENT_BACKFILL", "LAST_EVENT_ID", "88"),
                watermark("CDP_EVENT_AGGREGATE", "WINDOW_END", aggregateFrom.toString()));
        CdpWarehouseOperationsService service =
                service(mock(CdpWarehouseSyncRunMapper.class), watermarkMapper);

        CdpWarehouseOperationsService.OfflineCyclePlan plan =
                service.planOfflineCycle(9L, now, 100, 30);

        assertThat(plan.tenantId()).isEqualTo(9L);
        assertThat(plan.steps()).hasSize(2);
        assertThat(plan.steps().get(0).stepKey()).isEqualTo("BACKFILL");
        assertThat(plan.steps().get(0).status()).isEqualTo("READY");
        assertThat(plan.steps().get(0).sourceStartId()).isEqualTo(88L);
        assertThat(plan.steps().get(1).stepKey()).isEqualTo("AGGREGATE");
        assertThat(plan.steps().get(1).status()).isEqualTo("WAITING_FOR_BACKFILL");
        assertThat(plan.steps().get(1).windowStart()).isEqualTo(aggregateFrom);
        assertThat(plan.steps().get(1).windowEnd()).isEqualTo(now);
    }

    @Test
    void offlineCycleBlocksAggregationWhenBackfillFailsAndRecordsCycleRun() {
        CdpWarehouseWatermarkMapper watermarkMapper = mock(CdpWarehouseWatermarkMapper.class);
        CdpWarehouseSyncRunMapper runMapper = mock(CdpWarehouseSyncRunMapper.class);
        CdpWarehouseBackfillService backfill = mock(CdpWarehouseBackfillService.class);
        CdpWarehouseAggregationService aggregation = mock(CdpWarehouseAggregationService.class);
        LocalDateTime aggregateFrom = LocalDateTime.of(2026, 6, 5, 11, 30);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        when(watermarkMapper.selectOne(any())).thenReturn(
                watermark("CDP_EVENT_BACKFILL", "LAST_EVENT_ID", "88"),
                watermark("CDP_EVENT_AGGREGATE", "WINDOW_END", aggregateFrom.toString()));
        when(backfill.backfill(9L, 88L, 100, "operator"))
                .thenReturn(new CdpWarehouseBackfillService.BackfillResult("FAILED", 2, 1, 90));
        doAnswer(invocation -> {
            CdpWarehouseSyncRunDO run = invocation.getArgument(0);
            run.setId(501L);
            return 1;
        }).when(runMapper).insert(any(CdpWarehouseSyncRunDO.class));
        CdpWarehouseOperationsService service = service(backfill, aggregation, runMapper, watermarkMapper);

        CdpWarehouseOperationsService.OfflineCycleResult result =
                service.runOfflineCycle(9L, now, 100, 30, "operator");

        assertThat(result.runId()).isEqualTo(501L);
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.steps().get(1).status()).isEqualTo("BLOCKED");
        verify(aggregation, never()).aggregate(any(), any(), any(), any());
        verify(runMapper).updateById(org.mockito.ArgumentMatchers.argThat((CdpWarehouseSyncRunDO run) ->
                "OFFLINE_CYCLE".equals(run.getJobType())
                        && "FAILED".equals(run.getStatus())
                        && run.getSourceStartId().equals(88L)
                        && run.getSourceEndId().equals(90L)));
    }

    @Test
    void offlineCycleRunsAggregationAfterSuccessfulBackfill() {
        CdpWarehouseWatermarkMapper watermarkMapper = mock(CdpWarehouseWatermarkMapper.class);
        CdpWarehouseSyncRunMapper runMapper = mock(CdpWarehouseSyncRunMapper.class);
        CdpWarehouseBackfillService backfill = mock(CdpWarehouseBackfillService.class);
        CdpWarehouseAggregationService aggregation = mock(CdpWarehouseAggregationService.class);
        LocalDateTime aggregateFrom = LocalDateTime.of(2026, 6, 5, 11, 30);
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        when(watermarkMapper.selectOne(any())).thenReturn(
                watermark("CDP_EVENT_BACKFILL", "LAST_EVENT_ID", "88"),
                watermark("CDP_EVENT_AGGREGATE", "WINDOW_END", aggregateFrom.toString()));
        when(backfill.backfill(9L, 88L, 100, "operator"))
                .thenReturn(new CdpWarehouseBackfillService.BackfillResult("SUCCESS", 2, 0, 90));
        when(aggregation.aggregate(9L, aggregateFrom, now, "operator"))
                .thenReturn(new CdpWarehouseAggregationService.AggregationResult("SUCCESS", 3, 4));
        doAnswer(invocation -> {
            CdpWarehouseSyncRunDO run = invocation.getArgument(0);
            run.setId(502L);
            return 1;
        }).when(runMapper).insert(any(CdpWarehouseSyncRunDO.class));
        CdpWarehouseOperationsService service = service(backfill, aggregation, runMapper, watermarkMapper);

        CdpWarehouseOperationsService.OfflineCycleResult result =
                service.runOfflineCycle(9L, now, 100, 30, "operator");

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.loadedRows()).isEqualTo(9L);
        assertThat(result.steps()).extracting(CdpWarehouseOperationsService.OfflineCycleStepResult::status)
                .containsExactly("SUCCESS", "SUCCESS");
        verify(aggregation).aggregate(9L, aggregateFrom, now, "operator");
        verify(runMapper).updateById(org.mockito.ArgumentMatchers.argThat((CdpWarehouseSyncRunDO run) ->
                "OFFLINE_CYCLE".equals(run.getJobType())
                        && "SUCCESS".equals(run.getStatus())
                        && run.getLoadedRows().equals(9L)
                        && run.getFailedRows().equals(0L)));
    }

    private CdpWarehouseOperationsService service(CdpWarehouseSyncRunMapper runMapper,
                                                  CdpWarehouseWatermarkMapper watermarkMapper) {
        return service(mock(CdpWarehouseBackfillService.class), mock(CdpWarehouseAggregationService.class),
                runMapper, watermarkMapper);
    }

    private CdpWarehouseOperationsService service(CdpWarehouseBackfillService backfill,
                                                  CdpWarehouseAggregationService aggregation,
                                                  CdpWarehouseSyncRunMapper runMapper,
                                                  CdpWarehouseWatermarkMapper watermarkMapper) {
        return new CdpWarehouseOperationsService(backfill, aggregation, runMapper, watermarkMapper);
    }

    private CdpWarehouseSyncRunDO run(String jobType, String status) {
        CdpWarehouseSyncRunDO run = new CdpWarehouseSyncRunDO();
        run.setId(10L);
        run.setTenantId(9L);
        run.setJobType(jobType);
        run.setStatus(status);
        run.setLoadedRows(3L);
        run.setFailedRows(0L);
        run.setStartedAt(LocalDateTime.of(2026, 6, 5, 10, 0));
        run.setFinishedAt(LocalDateTime.of(2026, 6, 5, 10, 1));
        return run;
    }

    private CdpWarehouseWatermarkDO watermark(String jobName, String type, String value) {
        CdpWarehouseWatermarkDO watermark = new CdpWarehouseWatermarkDO();
        watermark.setId(20L);
        watermark.setTenantId(9L);
        watermark.setJobName(jobName);
        watermark.setWatermarkType(type);
        watermark.setWatermarkValue(value);
        watermark.setWatermarkTime(LocalDateTime.of(2026, 6, 5, 10, 0));
        return watermark;
    }
}
