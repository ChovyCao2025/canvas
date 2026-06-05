package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseQualityCheckDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseWatermarkDO;
import org.chovy.canvas.dal.mapper.CdpEventLogMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseQualityCheckMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseWatermarkMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class CdpWarehouseQualityService {

    private static final int MAX_RECENT_LIMIT = 100;
    private static final String CHECK_ODS_COUNT = "ODS_COUNT";
    private static final String CHECK_AGGREGATE_LAG = "AGGREGATE_LAG";
    private static final String PASS = "PASS";
    private static final String WARN = "WARN";
    private static final String SKIPPED = "SKIPPED";

    private final CdpEventLogMapper eventLogMapper;
    private final ObjectProvider<JdbcTemplate> dorisJdbcTemplate;
    private final CdpWarehouseQualityCheckMapper qualityCheckMapper;
    private final CdpWarehouseWatermarkMapper watermarkMapper;
    private final ObjectProvider<CdpWarehouseIncidentService> incidentService;

    public CdpWarehouseQualityService(CdpEventLogMapper eventLogMapper,
                                      @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
                                      CdpWarehouseQualityCheckMapper qualityCheckMapper,
                                      CdpWarehouseWatermarkMapper watermarkMapper) {
        this(eventLogMapper, dorisJdbcTemplate, qualityCheckMapper, watermarkMapper, null);
    }

    public CdpWarehouseQualityService(CdpEventLogMapper eventLogMapper,
                                      @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
                                      CdpWarehouseQualityCheckMapper qualityCheckMapper,
                                      CdpWarehouseWatermarkMapper watermarkMapper,
                                      ObjectProvider<CdpWarehouseIncidentService> incidentService) {
        this.eventLogMapper = eventLogMapper;
        this.dorisJdbcTemplate = dorisJdbcTemplate;
        this.qualityCheckMapper = qualityCheckMapper;
        this.watermarkMapper = watermarkMapper;
        this.incidentService = incidentService;
    }

    public QualityCheckResult reconcileOds(Long tenantId,
                                           LocalDateTime from,
                                           LocalDateTime to,
                                           long tolerance,
                                           String operator) {
        validateWindow(from, to);
        Long scopedTenantId = normalizeTenant(tenantId);
        long boundedTolerance = Math.max(tolerance, 0L);
        long sourceCount = countMysqlAccepted(scopedTenantId, from, to);
        JdbcTemplate doris = dorisJdbcTemplate == null ? null : dorisJdbcTemplate.getIfAvailable();
        if (doris == null) {
            CdpWarehouseQualityCheckDO row = newCheck(scopedTenantId, CHECK_ODS_COUNT, SKIPPED, operator);
            row.setSourceCount(sourceCount);
            row.setWindowStart(from);
            row.setWindowEnd(to);
            row.setThresholdValue(boundedTolerance);
            row.setDetailsJson("{\"reason\":\"doris disabled\"}");
            qualityCheckMapper.insert(row);
            QualityCheckResult result = toResult(row);
            recordIncidentIfNeeded(result);
            return result;
        }

        long warehouseCount = countDorisOds(doris, scopedTenantId, from, to);
        long diff = Math.abs(sourceCount - warehouseCount);
        String status = diff <= boundedTolerance ? PASS : WARN;
        CdpWarehouseQualityCheckDO row = newCheck(scopedTenantId, CHECK_ODS_COUNT, status, operator);
        row.setSourceCount(sourceCount);
        row.setWarehouseCount(warehouseCount);
        row.setDiffCount(diff);
        row.setWindowStart(from);
        row.setWindowEnd(to);
        row.setThresholdValue(boundedTolerance);
        row.setDetailsJson("{\"source\":\"mysql.cdp_event_log\",\"warehouse\":\"canvas_ods.cdp_event_log\"}");
        qualityCheckMapper.insert(row);
        QualityCheckResult result = toResult(row);
        recordIncidentIfNeeded(result);
        return result;
    }

    public QualityCheckResult checkAggregateLag(Long tenantId,
                                                LocalDateTime now,
                                                long maxLagMinutes,
                                                String operator) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime effectiveNow = now == null ? LocalDateTime.now() : now;
        long threshold = Math.max(maxLagMinutes, 0L);
        CdpWarehouseQualityCheckDO row = newCheck(scopedTenantId, CHECK_AGGREGATE_LAG, WARN, operator);
        row.setWindowEnd(effectiveNow);
        row.setThresholdValue(threshold);

        CdpWarehouseWatermarkDO watermark = watermarkMapper.selectOne(new LambdaQueryWrapper<CdpWarehouseWatermarkDO>()
                .eq(CdpWarehouseWatermarkDO::getTenantId, scopedTenantId)
                .eq(CdpWarehouseWatermarkDO::getJobName, "CDP_EVENT_AGGREGATE")
                .eq(CdpWarehouseWatermarkDO::getWatermarkType, "WINDOW_END")
                .last("LIMIT 1"));
        LocalDateTime watermarkTime = parseWatermarkTime(watermark);
        if (watermarkTime == null) {
            row.setDetailsJson("{\"reason\":\"missing aggregate watermark\"}");
            qualityCheckMapper.insert(row);
            QualityCheckResult result = toResult(row);
            recordIncidentIfNeeded(result);
            return result;
        }

        long lagMinutes = Math.max(0L, ChronoUnit.MINUTES.between(watermarkTime, effectiveNow));
        row.setStatus(lagMinutes <= threshold ? PASS : WARN);
        row.setDiffCount(lagMinutes);
        row.setWindowStart(watermarkTime);
        row.setDetailsJson("{\"watermark\":\"" + watermarkTime + "\",\"lagMinutes\":" + lagMinutes + "}");
        qualityCheckMapper.insert(row);
        QualityCheckResult result = toResult(row);
        recordIncidentIfNeeded(result);
        return result;
    }

    private void recordIncidentIfNeeded(QualityCheckResult result) {
        CdpWarehouseIncidentService service = incidentService == null ? null : incidentService.getIfAvailable();
        if (service == null) {
            return;
        }
        try {
            service.recordQualityIncident(result);
        } catch (RuntimeException ex) {
            log.warn("[WAREHOUSE_QUALITY] incident side effect failed tenantId={} checkType={}: {}",
                    result.tenantId(), result.checkType(), ex.getMessage());
        }
    }

    public List<QualityCheckResult> recentChecks(Long tenantId, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int boundedLimit = boundLimit(limit);
        List<CdpWarehouseQualityCheckDO> rows = qualityCheckMapper.selectList(
                new LambdaQueryWrapper<CdpWarehouseQualityCheckDO>()
                        .eq(CdpWarehouseQualityCheckDO::getTenantId, scopedTenantId)
                        .orderByDesc(CdpWarehouseQualityCheckDO::getCheckedAt)
                        .orderByDesc(CdpWarehouseQualityCheckDO::getId)
                        .last("LIMIT " + boundedLimit));
        return rows == null ? List.of() : rows.stream().map(this::toResult).toList();
    }

    private long countMysqlAccepted(Long tenantId, LocalDateTime from, LocalDateTime to) {
        Long count = eventLogMapper.selectCount(new LambdaQueryWrapper<CdpEventLogDO>()
                .eq(CdpEventLogDO::getTenantId, tenantId)
                .eq(CdpEventLogDO::getStatus, CdpEventLogDO.ACCEPTED)
                .ge(CdpEventLogDO::getReceivedAt, from)
                .lt(CdpEventLogDO::getReceivedAt, to));
        return count == null ? 0L : count;
    }

    private long countDorisOds(JdbcTemplate doris, Long tenantId, LocalDateTime from, LocalDateTime to) {
        Long count = doris.queryForObject("""
                SELECT COUNT(1)
                FROM canvas_ods.cdp_event_log
                WHERE tenant_id = ?
                  AND received_at >= ?
                  AND received_at < ?
                """, Long.class, tenantId, from, to);
        return count == null ? 0L : count;
    }

    private CdpWarehouseQualityCheckDO newCheck(Long tenantId, String checkType, String status, String operator) {
        CdpWarehouseQualityCheckDO row = new CdpWarehouseQualityCheckDO();
        row.setTenantId(tenantId);
        row.setCheckType(checkType);
        row.setStatus(status);
        row.setCheckedAt(LocalDateTime.now());
        row.setCreatedBy(normalizeOperator(operator));
        return row;
    }

    private QualityCheckResult toResult(CdpWarehouseQualityCheckDO row) {
        return new QualityCheckResult(
                row.getId(),
                row.getTenantId(),
                row.getCheckType(),
                row.getStatus(),
                row.getSourceCount(),
                row.getWarehouseCount(),
                row.getDiffCount(),
                row.getWindowStart(),
                row.getWindowEnd(),
                row.getThresholdValue(),
                row.getDetailsJson(),
                row.getCheckedAt(),
                row.getCreatedBy());
    }

    private LocalDateTime parseWatermarkTime(CdpWarehouseWatermarkDO watermark) {
        if (watermark == null) {
            return null;
        }
        if (watermark.getWatermarkValue() != null && !watermark.getWatermarkValue().isBlank()) {
            try {
                return LocalDateTime.parse(watermark.getWatermarkValue().trim());
            } catch (RuntimeException ignored) {
                return watermark.getWatermarkTime();
            }
        }
        return watermark.getWatermarkTime();
    }

    private void validateWindow(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
    }

    private int boundLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, MAX_RECENT_LIMIT);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String normalizeOperator(String operator) {
        if (operator == null || operator.isBlank()) {
            return "operator";
        }
        return operator;
    }

    public record QualityCheckResult(
            Long id,
            Long tenantId,
            String checkType,
            String status,
            Long sourceCount,
            Long warehouseCount,
            Long diffCount,
            LocalDateTime windowStart,
            LocalDateTime windowEnd,
            Long thresholdValue,
            String details,
            LocalDateTime checkedAt,
            String createdBy) {
    }
}
