package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseSyncRunDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseWatermarkDO;
import org.chovy.canvas.dal.mapper.CdpEventLogMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseSyncRunMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseWatermarkMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CdpWarehouseBackfillService {

    private static final String JOB_TYPE = "BACKFILL";
    private static final String SOURCE_TABLE = "cdp_event_log";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final int MAX_ERROR_LENGTH = 1000;

    private final CdpEventLogMapper eventLogMapper;
    private final CdpWarehouseEventSink eventSink;
    private final CdpWarehouseSyncRunMapper runMapper;
    private final CdpWarehouseWatermarkMapper watermarkMapper;

    public BackfillResult backfill(Long tenantId, Long lastId, int limit, String operator) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        long startId = lastId == null ? 0L : lastId;
        CdpWarehouseSyncRunDO run = newRun(scopedTenantId, startId, operator);
        runMapper.insert(run);

        List<CdpEventLogDO> rows = eventLogMapper.selectList(new LambdaQueryWrapper<CdpEventLogDO>()
                .eq(CdpEventLogDO::getTenantId, scopedTenantId)
                .eq(CdpEventLogDO::getStatus, CdpEventLogDO.ACCEPTED)
                .gt(CdpEventLogDO::getId, startId)
                .orderByAsc(CdpEventLogDO::getId)
                .last("LIMIT " + Math.min(limit, 5000)));

        long loaded = 0L;
        long failed = 0L;
        long maxId = startId;
        String error = null;
        for (CdpEventLogDO row : rows) {
            try {
                eventSink.writeAccepted(row);
                loaded++;
                if (row.getId() != null && row.getId() > maxId) {
                    maxId = row.getId();
                }
            } catch (RuntimeException ex) {
                failed++;
                error = limit(ex.getMessage());
            }
        }

        run.setSourceEndId(maxId);
        run.setLoadedRows(loaded);
        run.setFailedRows(failed);
        run.setStatus(failed > 0 ? STATUS_FAILED : STATUS_SUCCESS);
        run.setErrorMessage(error);
        run.setFinishedAt(LocalDateTime.now());
        runMapper.updateById(run);
        if (loaded > 0) {
            upsertWatermark(scopedTenantId, maxId);
        }
        return new BackfillResult(run.getStatus(), loaded, failed, maxId);
    }

    private CdpWarehouseSyncRunDO newRun(Long tenantId, Long startId, String operator) {
        CdpWarehouseSyncRunDO run = new CdpWarehouseSyncRunDO();
        run.setTenantId(tenantId);
        run.setJobType(JOB_TYPE);
        run.setSourceTable(SOURCE_TABLE);
        run.setSourceStartId(startId);
        run.setStatus(STATUS_RUNNING);
        run.setLoadedRows(0L);
        run.setFailedRows(0L);
        run.setStartedAt(LocalDateTime.now());
        run.setCreatedBy(operator);
        return run;
    }

    private void upsertWatermark(Long tenantId, Long maxId) {
        CdpWarehouseWatermarkDO watermark = new CdpWarehouseWatermarkDO();
        watermark.setTenantId(tenantId);
        watermark.setJobName("CDP_EVENT_BACKFILL");
        watermark.setWatermarkType("LAST_EVENT_ID");
        watermark.setWatermarkValue(String.valueOf(maxId));
        watermark.setWatermarkTime(LocalDateTime.now());
        watermarkMapper.upsert(watermark);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String limit(String message) {
        String value = message == null ? "warehouse backfill failed" : message;
        if (value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    public record BackfillResult(String status, long loaded, long failed, long lastEventId) {
    }
}
