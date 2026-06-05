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

    public CdpWarehouseAggregationService(
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            CdpWarehouseSyncRunMapper runMapper,
            CdpWarehouseWatermarkMapper watermarkMapper) {
        this(dorisJdbcTemplate, runMapper, watermarkMapper, null);
    }

    public AggregationResult aggregate(Long tenantId, LocalDateTime from, LocalDateTime to, String operator) {
        validateWindow(from, to);
        JdbcTemplate doris = dorisJdbcTemplate.getIfAvailable();
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
            return new AggregationResult(STATUS_FAILED, 0, 0);
        }
    }

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

    private void upsertWatermark(Long tenantId, LocalDateTime watermarkTime) {
        CdpWarehouseWatermarkDO watermark = new CdpWarehouseWatermarkDO();
        watermark.setTenantId(tenantId);
        watermark.setJobName("CDP_EVENT_AGGREGATE");
        watermark.setWatermarkType("WINDOW_END");
        watermark.setWatermarkValue(watermarkTime.toString());
        watermark.setWatermarkTime(watermarkTime);
        watermarkMapper.upsert(watermark);
    }

    private void recordOfflineAssetAvailability(Long tenantId,
                                                CdpWarehouseSyncRunDO run,
                                                LocalDateTime from,
                                                LocalDateTime to) {
        CdpWarehouseConsumerAvailabilityService service = consumerAvailabilityService();
        if (service == null) {
            return;
        }
        try {
            LocalDateTime observedAt = run.getFinishedAt() == null ? LocalDateTime.now() : run.getFinishedAt();
            String evidenceRef = "run:" + (run.getId() == null ? "unknown" : run.getId());
            String reason = "offline aggregation completed for window " + from + " to " + to;
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

    private CdpWarehouseConsumerAvailabilityService consumerAvailabilityService() {
        return consumerAvailabilityServiceProvider == null ? null : consumerAvailabilityServiceProvider.getIfAvailable();
    }

    private void validateWindow(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String limit(String message) {
        String value = message == null ? "warehouse aggregation failed" : message;
        if (value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    public record AggregationResult(String status, int dwdRows, int dwsRows) {
    }

    private record AvailabilityAsset(String assetType, String assetKey) {
    }
}
