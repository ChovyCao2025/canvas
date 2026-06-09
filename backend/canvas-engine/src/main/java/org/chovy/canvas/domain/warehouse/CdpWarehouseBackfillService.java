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
/**
 * CdpWarehouseBackfillService 承载对应领域的业务规则、流程编排和结果转换。
 */
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

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param lastId 业务对象 ID，用于定位具体记录。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 backfill 流程生成的业务结果。
     */
    public BackfillResult backfill(Long tenantId, Long lastId, int limit, String operator) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        long startId = lastId == null ? 0L : lastId;
        CdpWarehouseSyncRunDO run = newRun(scopedTenantId, startId, operator);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param startId 业务对象 ID，用于定位具体记录。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 newRun 流程生成的业务结果。
     */
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

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param maxId 业务对象 ID，用于定位具体记录。
     */
    private void upsertWatermark(Long tenantId, Long maxId) {
        CdpWarehouseWatermarkDO watermark = new CdpWarehouseWatermarkDO();
        watermark.setTenantId(tenantId);
        watermark.setJobName("CDP_EVENT_BACKFILL");
        watermark.setWatermarkType("LAST_EVENT_ID");
        watermark.setWatermarkValue(String.valueOf(maxId));
        watermark.setWatermarkTime(LocalDateTime.now());
        watermarkMapper.upsert(watermark);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String limit(String message) {
        String value = message == null ? "warehouse backfill failed" : message;
        if (value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    /**
     * BackfillResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record BackfillResult(String status, long loaded, long failed, long lastEventId) {
    }
}
