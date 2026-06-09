package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseSyncRunDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseWatermarkDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseSyncRunMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseWatermarkMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
/**
 * CdpWarehouseAggregationService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseAggregationService {

    private static final String JOB_TYPE = "AGGREGATE";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_SKIPPED = "SKIPPED";
    private static final String AVAILABILITY_STATUS_PASS = "PASS";
    private static final String AVAILABILITY_MODE_OFFLINE = "OFFLINE";
    private static final String EVIDENCE_SOURCE_AGGREGATE_JOB = "AGGREGATE_JOB";
    private static final int MAX_ERROR_LENGTH = 1000;
    private static final List<AvailabilityAsset> OFFLINE_AGGREGATION_ASSETS = List.of(
            new AvailabilityAsset("TABLE", "canvas_dwd.cdp_user_event_fact"),
            new AvailabilityAsset("TABLE", "canvas_dws.user_event_metric_daily"),
            new AvailabilityAsset("DATASET", "cdp_dwd_user_event_fact"),
            new AvailabilityAsset("DATASET", "cdp_dws_user_event_metric_daily"));

    private final ObjectProvider<JdbcTemplate> dorisJdbcTemplate;
    private final CdpWarehouseSyncRunMapper runMapper;
    private final CdpWarehouseWatermarkMapper watermarkMapper;
    private final ObjectProvider<CdpWarehouseConsumerAvailabilityService> consumerAvailabilityServiceProvider;

    @Autowired
    /**
     * 初始化 CdpWarehouseAggregationService 实例。
     *
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 CdpWarehouseAggregationService 流程中的校验、计算或对象转换。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param watermarkMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseAggregationService(
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            CdpWarehouseSyncRunMapper runMapper,
            CdpWarehouseWatermarkMapper watermarkMapper,
            ObjectProvider<CdpWarehouseConsumerAvailabilityService> consumerAvailabilityServiceProvider) {
        this.dorisJdbcTemplate = dorisJdbcTemplate;
        this.runMapper = runMapper;
        this.watermarkMapper = watermarkMapper;
        this.consumerAvailabilityServiceProvider = consumerAvailabilityServiceProvider;
    }

    /**
     * 初始化 CdpWarehouseAggregationService 实例。
     *
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 CdpWarehouseAggregationService 流程中的校验、计算或对象转换。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param watermarkMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseAggregationService(
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            CdpWarehouseSyncRunMapper runMapper,
            CdpWarehouseWatermarkMapper watermarkMapper) {
        this(dorisJdbcTemplate, runMapper, watermarkMapper, null);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 aggregate 流程生成的业务结果。
     */
    public AggregationResult aggregate(Long tenantId, LocalDateTime from, LocalDateTime to, String operator) {
        validateWindow(from, to);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        JdbcTemplate doris = dorisJdbcTemplate.getIfAvailable();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (doris == null) {
            return new AggregationResult(STATUS_SKIPPED, 0, 0);
        }

        Long scopedTenantId = normalizeTenant(tenantId);
        CdpWarehouseSyncRunDO run = newRun(scopedTenantId, from, to, operator);
        runMapper.insert(run);
        try {
            int dwdRows = doris.update(dwdSql(), scopedTenantId, from, to);
            int dwsRows = doris.update(dwsSql(), scopedTenantId, from.toLocalDate(), to.toLocalDate());
            run.setStatus(STATUS_SUCCESS);
            run.setLoadedRows((long) dwdRows + dwsRows);
            run.setFinishedAt(LocalDateTime.now());
            runMapper.updateById(run);
            upsertWatermark(scopedTenantId, to);
            recordOfflineAssetAvailability(scopedTenantId, run, from, to);
            return new AggregationResult(STATUS_SUCCESS, dwdRows, dwsRows);
        } catch (RuntimeException ex) {
            run.setStatus(STATUS_FAILED);
            run.setErrorMessage(limit(ex.getMessage()));
            run.setFinishedAt(LocalDateTime.now());
            runMapper.updateById(run);
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new AggregationResult(STATUS_FAILED, 0, 0);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 dwd sql 生成的文本或业务键。
     */
    private String dwdSql() {
        return """
                INSERT INTO canvas_dwd.cdp_user_event_fact
                (tenant_id, user_id, event_code, event_time, channel, canvas_id, node_id, properties_json, event_date)
                SELECT tenant_id,
                       COALESCE(NULLIF(user_id, ''), anonymous_id) AS user_id,
                       event_code,
                       event_time,
                       JSON_UNQUOTE(JSON_EXTRACT(properties, '$.channel')) AS channel,
                       CAST(JSON_UNQUOTE(JSON_EXTRACT(properties, '$.canvasId')) AS BIGINT) AS canvas_id,
                       JSON_UNQUOTE(JSON_EXTRACT(properties, '$.nodeId')) AS node_id,
                       CAST(properties AS STRING) AS properties_json,
                       DATE(event_time) AS event_date
                FROM canvas_ods.cdp_event_log
                WHERE tenant_id = ?
                  AND received_at >= ?
                  AND received_at < ?
                  AND event_code IS NOT NULL
                  AND COALESCE(NULLIF(user_id, ''), anonymous_id) IS NOT NULL
                """;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 dws sql 生成的文本或业务键。
     */
    private String dwsSql() {
        return """
                INSERT INTO canvas_dws.user_event_metric_daily
                (stat_date, tenant_id, user_id, event_code, count_value, numeric_sum, max_numeric, latest_event_time)
                SELECT event_date AS stat_date,
                       tenant_id,
                       user_id,
                       event_code,
                       COUNT(1) AS count_value,
                       0 AS numeric_sum,
                       NULL AS max_numeric,
                       MAX(event_time) AS latest_event_time
                FROM canvas_dwd.cdp_user_event_fact
                WHERE tenant_id = ?
                  AND event_date >= ?
                  AND event_date <= ?
                GROUP BY event_date, tenant_id, user_id, event_code
                """;
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 newRun 流程生成的业务结果。
     */
    private CdpWarehouseSyncRunDO newRun(Long tenantId, LocalDateTime from, LocalDateTime to, String operator) {
        CdpWarehouseSyncRunDO run = new CdpWarehouseSyncRunDO();
        run.setTenantId(tenantId);
        run.setJobType(JOB_TYPE);
        run.setWindowStart(from);
        run.setWindowEnd(to);
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
     * @param watermarkTime 时间参数，用于计算窗口、过期或审计时间。
     */
    private void upsertWatermark(Long tenantId, LocalDateTime watermarkTime) {
        CdpWarehouseWatermarkDO watermark = new CdpWarehouseWatermarkDO();
        watermark.setTenantId(tenantId);
        watermark.setJobName("CDP_EVENT_AGGREGATE");
        watermark.setWatermarkType("WINDOW_END");
        watermark.setWatermarkValue(watermarkTime.toString());
        watermark.setWatermarkTime(watermarkTime);
        watermarkMapper.upsert(watermark);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param run run 参数，用于 recordOfflineAssetAvailability 流程中的校验、计算或对象转换。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     */
    private void recordOfflineAssetAvailability(Long tenantId,
                                                CdpWarehouseSyncRunDO run,
                                                LocalDateTime from,
                                                LocalDateTime to) {
        CdpWarehouseConsumerAvailabilityService service = consumerAvailabilityService();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (service == null) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        try {
            LocalDateTime observedAt = run.getFinishedAt() == null ? LocalDateTime.now() : run.getFinishedAt();
            String evidenceRef = "run:" + (run.getId() == null ? "unknown" : run.getId());
            String reason = "offline aggregation completed for window " + from + " to " + to;
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (AvailabilityAsset asset : OFFLINE_AGGREGATION_ASSETS) {
                service.recordAssetAvailability(tenantId,
                        new CdpWarehouseConsumerAvailabilityService.AssetAvailabilityCommand(
                                asset.assetType(),
                                asset.assetKey(),
                                AVAILABILITY_MODE_OFFLINE,
                                from,
                                to,
                                to,
                                AVAILABILITY_STATUS_PASS,
                                EVIDENCE_SOURCE_AGGREGATE_JOB,
                                evidenceRef,
                                reason,
                                observedAt));
            }
        } catch (RuntimeException ex) {
            log.warn("[WAREHOUSE_AGGREGATE] asset availability side effect failed tenantId={} runId={}: {}",
                    tenantId, run.getId(), ex.getMessage());
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 consumerAvailabilityService 流程生成的业务结果。
     */
    private CdpWarehouseConsumerAvailabilityService consumerAvailabilityService() {
        return consumerAvailabilityServiceProvider == null ? null : consumerAvailabilityServiceProvider.getIfAvailable();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     */
    private void validateWindow(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
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
        String value = message == null ? "warehouse aggregation failed" : message;
        if (value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    /**
     * AggregationResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AggregationResult(String status, int dwdRows, int dwsRows) {
    }

    /**
     * AvailabilityAsset 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record AvailabilityAsset(String assetType, String assetKey) {
    }
}
