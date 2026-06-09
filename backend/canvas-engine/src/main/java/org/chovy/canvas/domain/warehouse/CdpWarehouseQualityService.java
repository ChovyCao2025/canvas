package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseQualityCheckDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseWatermarkDO;
import org.chovy.canvas.dal.mapper.CdpEventLogMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseQualityCheckMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseWatermarkMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
/**
 * CdpWarehouseQualityService 承载对应领域的业务规则、流程编排和结果转换。
 */
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

    /**
     * 初始化 CdpWarehouseQualityService 实例。
     *
     * @param eventLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 CdpWarehouseQualityService 流程中的校验、计算或对象转换。
     * @param qualityCheckMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param watermarkMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseQualityService(CdpEventLogMapper eventLogMapper,
                                      @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
                                      CdpWarehouseQualityCheckMapper qualityCheckMapper,
                                      CdpWarehouseWatermarkMapper watermarkMapper) {
        this(eventLogMapper, dorisJdbcTemplate, qualityCheckMapper, watermarkMapper, null);
    }

    @Autowired
    /**
     * 初始化 CdpWarehouseQualityService 实例。
     *
     * @param eventLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 CdpWarehouseQualityService 流程中的校验、计算或对象转换。
     * @param qualityCheckMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param watermarkMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param incidentService 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param tolerance tolerance 参数，用于 reconcileOds 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 reconcileOds 流程生成的业务结果。
     */
    public QualityCheckResult reconcileOds(Long tenantId,
                                           LocalDateTime from,
                                           LocalDateTime to,
                                           long tolerance,
                                           String operator) {
        validateWindow(from, to);
        Long scopedTenantId = normalizeTenant(tenantId);
        long boundedTolerance = Math.max(tolerance, 0L);
        long sourceCount = countMysqlAccepted(scopedTenantId, from, to);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        JdbcTemplate doris = dorisJdbcTemplate == null ? null : dorisJdbcTemplate.getIfAvailable();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param maxLagMinutes max lag minutes 参数，用于 checkAggregateLag 流程中的校验、计算或对象转换。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回布尔判断结果。
     */
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

        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CdpWarehouseWatermarkDO watermark = watermarkMapper.selectOne(new LambdaQueryWrapper<CdpWarehouseWatermarkDO>()
                .eq(CdpWarehouseWatermarkDO::getTenantId, scopedTenantId)
                .eq(CdpWarehouseWatermarkDO::getJobName, "CDP_EVENT_AGGREGATE")
                .eq(CdpWarehouseWatermarkDO::getWatermarkType, "WINDOW_END")
                .last("LIMIT 1"));
        LocalDateTime watermarkTime = parseWatermarkTime(watermark);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param result result 参数，用于 recordIncidentIfNeeded 流程中的校验、计算或对象转换。
     */
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

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
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

    /**
     * 统计符合条件的数据规模或状态数量。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @return 返回统计数量。
     */
    private long countMysqlAccepted(Long tenantId, LocalDateTime from, LocalDateTime to) {
        Long count = eventLogMapper.selectCount(new LambdaQueryWrapper<CdpEventLogDO>()
                .eq(CdpEventLogDO::getTenantId, tenantId)
                .eq(CdpEventLogDO::getStatus, CdpEventLogDO.ACCEPTED)
                .ge(CdpEventLogDO::getReceivedAt, from)
                .lt(CdpEventLogDO::getReceivedAt, to));
        return count == null ? 0L : count;
    }

    /**
     * 统计符合条件的数据规模或状态数量。
     *
     * @param doris doris 参数，用于 countDorisOds 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @return 返回统计数量。
     */
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

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param checkType 类型标识，用于选择对应处理分支。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 newCheck 流程生成的业务结果。
     */
    private CdpWarehouseQualityCheckDO newCheck(Long tenantId, String checkType, String status, String operator) {
        CdpWarehouseQualityCheckDO row = new CdpWarehouseQualityCheckDO();
        row.setTenantId(tenantId);
        row.setCheckType(checkType);
        row.setStatus(status);
        row.setCheckedAt(LocalDateTime.now());
        row.setCreatedBy(normalizeOperator(operator));
        return row;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param watermark watermark 参数，用于 parseWatermarkTime 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private LocalDateTime parseWatermarkTime(CdpWarehouseWatermarkDO watermark) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return watermark.getWatermarkTime();
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
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, MAX_RECENT_LIMIT);
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
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOperator(String operator) {
        if (operator == null || operator.isBlank()) {
            return "operator";
        }
        return operator;
    }

    /**
     * QualityCheckResult 承载对应领域的业务规则、流程编排和结果转换。
     */
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
