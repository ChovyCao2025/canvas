package org.chovy.canvas.domain.analytics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.dal.dataobject.AnalyticsAlertRuleDO;
import org.chovy.canvas.dal.dataobject.AnalyticsExportJobDO;
import org.chovy.canvas.dal.dataobject.AnalyticsFunnelDefinitionDO;
import org.chovy.canvas.dal.mapper.AnalyticsAlertRuleMapper;
import org.chovy.canvas.dal.mapper.AnalyticsEventMapper;
import org.chovy.canvas.dal.mapper.AnalyticsExportJobMapper;
import org.chovy.canvas.dal.mapper.AnalyticsFunnelDefinitionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AnalyticsQueryService 承载对应领域的业务规则、流程编排和结果转换。
 */
@Service
public class AnalyticsQueryService {

    private static final int DEFAULT_EXPORT_ROW_LIMIT = 100_000;
    private static final TypeReference<List<Map<String, Object>>> STEP_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final AnalyticsEventMapper eventMapper;
    private final AnalyticsQueryGuard guard;
    private final AnalyticsFunnelDefinitionMapper funnelDefinitionMapper;
    private final AnalyticsAlertRuleMapper alertRuleMapper;
    private final AnalyticsExportJobMapper exportJobMapper;
    private final ObjectMapper objectMapper;
    private final int maxExportRows;

    public AnalyticsQueryService(AnalyticsEventMapper eventMapper, AnalyticsQueryGuard guard) {
        this(eventMapper, guard, null, null, null, new ObjectMapper(), DEFAULT_EXPORT_ROW_LIMIT);
    }

    @Autowired
    public AnalyticsQueryService(AnalyticsEventMapper eventMapper,
                                 AnalyticsQueryGuard guard,
                                 AnalyticsFunnelDefinitionMapper funnelDefinitionMapper,
                                 AnalyticsAlertRuleMapper alertRuleMapper,
                                 AnalyticsExportJobMapper exportJobMapper,
                                 ObjectMapper objectMapper) {
        this(eventMapper,
                guard,
                funnelDefinitionMapper,
                alertRuleMapper,
                exportJobMapper,
                objectMapper,
                DEFAULT_EXPORT_ROW_LIMIT);
    }

    AnalyticsQueryService(AnalyticsEventMapper eventMapper,
                          AnalyticsQueryGuard guard,
                          AnalyticsFunnelDefinitionMapper funnelDefinitionMapper,
                          AnalyticsAlertRuleMapper alertRuleMapper,
                          AnalyticsExportJobMapper exportJobMapper,
                          ObjectMapper objectMapper,
                          int maxExportRows) {
        this.eventMapper = eventMapper;
        this.guard = guard;
        this.funnelDefinitionMapper = funnelDefinitionMapper;
        this.alertRuleMapper = alertRuleMapper;
        this.exportJobMapper = exportJobMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.maxExportRows = maxExportRows < 1 ? DEFAULT_EXPORT_ROW_LIMIT : maxExportRows;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param startDate 时间参数，用于计算窗口、过期或审计时间。
     * @param endDate 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 event counts 汇总后的集合、分页或映射视图。
     */
    public List<EventCountRow> eventCounts(Long tenantId, String startDate, String endDate) {
        Long scopedTenantId = guard.requireTenantId(tenantId);
        AnalyticsQueryGuard.DateRange range = guard.validateDateRange(startDate, endDate);
        return eventMapper.selectEventCounts(scopedTenantId, range.startDate(), range.endDate())
                .stream()
                .map(row -> new EventCountRow(asString(row.get("eventCode")), asLong(row.get("count"))))
                .toList();
    }

    /**
     * 统计符合条件的数据规模或状态数量。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param startDate 时间参数，用于计算窗口、过期或审计时间。
     * @param endDate 时间参数，用于计算窗口、过期或审计时间。
     * @param eventCode 业务编码，用于匹配对应类型或状态。
     * @return 返回统计数量。
     */
    public EventTotal countEvents(Long tenantId, String startDate, String endDate, String eventCode) {
        Long scopedTenantId = guard.requireTenantId(tenantId);
        AnalyticsQueryGuard.DateRange range = guard.validateDateRange(startDate, endDate);
        long count = eventCode == null || eventCode.isBlank()
                ? eventMapper.countEvents(scopedTenantId, range.startDate(), range.endDate())
                : eventMapper.countByEventCode(scopedTenantId, guard.requireEventCode(eventCode), range.startDate(), range.endDate());
        return new EventTotal(count);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param startDate 时间参数，用于计算窗口、过期或审计时间。
     * @param endDate 时间参数，用于计算窗口、过期或审计时间。
     * @param page 分页或数量限制，避免一次处理过多数据。
     * @param size 分页或数量限制，避免一次处理过多数据。
     * @return 返回 user timeline 汇总后的集合、分页或映射视图。
     */
    public PageResult<UserTimelineRow> userTimeline(Long tenantId,
                                                    String userId,
                                                    String startDate,
                                                    String endDate,
                                                    Integer page,
                                                    Integer size) {
        Long scopedTenantId = guard.requireTenantId(tenantId);
        String scopedUserId = guard.requireUserId(userId);
        AnalyticsQueryGuard.DateRange range = guard.validateDateRange(startDate, endDate);
        AnalyticsQueryGuard.PageRequest pageRequest = guard.normalizePageRequest(page, size);
        List<UserTimelineRow> rows = eventMapper.selectUserTimeline(
                        scopedTenantId,
                        scopedUserId,
                        range.startDate(),
                        range.endDate(),
                        pageRequest.offset(),
                        pageRequest.size())
                .stream()
                .map(row -> new UserTimelineRow(asString(row.get("eventCode")), asString(row.get("eventTime"))))
                .toList();
        long total = eventMapper.countUserTimeline(scopedTenantId, scopedUserId, range.startDate(), range.endDate());
        return PageResult.of(total, rows);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param attribute attribute 参数，用于 attributeDistribution 流程中的校验、计算或对象转换。
     * @param startDate 时间参数，用于计算窗口、过期或审计时间。
     * @param endDate 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 attribute distribution 汇总后的集合、分页或映射视图。
     */
    public List<AttributeDistributionRow> attributeDistribution(Long tenantId,
                                                                String attribute,
                                                                String startDate,
                                                                String endDate) {
        Long scopedTenantId = guard.requireTenantId(tenantId);
        String attributePath = guard.requireAttributePath(attribute);
        AnalyticsQueryGuard.DateRange range = guard.validateDateRange(startDate, endDate);
        return eventMapper.selectAttributeDistribution(scopedTenantId, attributePath, range.startDate(), range.endDate())
                .stream()
                .map(row -> new AttributeDistributionRow(asString(row.get("value")), asLong(row.get("count"))))
                .toList();
    }

    public FunnelResult funnelResult(Long tenantId, String funnelKey, String startDate, String endDate) {
        Long scopedTenantId = guard.requireTenantId(tenantId);
        String scopedFunnelKey = requireKey("funnelKey", funnelKey);
        AnalyticsQueryGuard.DateRange range = guard.validateDateRange(startDate, endDate);
        requireFunnelMapper();

        AnalyticsFunnelDefinitionDO row = funnelDefinitionMapper.selectLatestEnabled(scopedTenantId, scopedFunnelKey);
        if (row == null) {
            throw new IllegalArgumentException("funnel definition not found");
        }
        return new FunnelResult(
                row.getFunnelKey(),
                row.getVersion() == null ? 0 : row.getVersion(),
                row.getName(),
                range.startDate(),
                range.endDate(),
                readSteps(row.getStepsJson()));
    }

    public AlertPreviewResult alertPreview(Long tenantId, AlertPreviewRequest request) {
        Long scopedTenantId = guard.requireTenantId(tenantId);
        AlertPreviewRequest scopedRequest = requireAlertPreviewRequest(request);
        AnalyticsQueryGuard.DateRange range = guard.validateDateRange(scopedRequest.startDate(), scopedRequest.endDate());
        String eventCode = guard.requireEventCode(scopedRequest.eventCode());
        requireAlertRuleMapper();

        AnalyticsAlertRuleDO row = alertRuleMapper.selectOne(new LambdaQueryWrapper<AnalyticsAlertRuleDO>()
                .eq(AnalyticsAlertRuleDO::getTenantId, scopedTenantId)
                .eq(AnalyticsAlertRuleDO::getRuleKey, scopedRequest.ruleKey())
                .eq(AnalyticsAlertRuleDO::getEnabled, true)
                .last("LIMIT 1"));
        if (row == null) {
            throw new IllegalArgumentException("alert rule not found");
        }
        long count = eventMapper.countByEventCode(scopedTenantId, eventCode, range.startDate(), range.endDate());
        long threshold = thresholdValue(row.getThresholdJson());
        return new AlertPreviewResult(
                row.getRuleKey(),
                row.getName(),
                eventCode,
                range.startDate(),
                range.endDate(),
                count,
                threshold,
                count >= threshold);
    }

    public ExportJobView createExport(Long tenantId, ExportRequest request) {
        Long scopedTenantId = guard.requireTenantId(tenantId);
        ExportRequest scopedRequest = requireExportRequest(request);
        AnalyticsQueryGuard.DateRange range = guard.validateDateRange(scopedRequest.startDate(), scopedRequest.endDate());
        String eventCode = scopedRequest.eventCode() == null || scopedRequest.eventCode().isBlank()
                ? null
                : guard.requireEventCode(scopedRequest.eventCode());
        int rowLimit = normalizeRowLimit(scopedRequest.rowLimit());
        long estimatedRows = eventCode == null
                ? eventMapper.countEvents(scopedTenantId, range.startDate(), range.endDate())
                : eventMapper.countByEventCode(scopedTenantId, eventCode, range.startDate(), range.endDate());
        if (estimatedRows > rowLimit) {
            throw new IllegalArgumentException("export row limit exceeded");
        }
        requireExportJobMapper();

        AnalyticsExportJobDO row = new AnalyticsExportJobDO();
        row.setTenantId(scopedTenantId);
        row.setReportType(scopedRequest.reportType());
        row.setQueryJson(toJson(Map.of(
                "reportType", scopedRequest.reportType(),
                "startDate", range.startDate(),
                "endDate", range.endDate(),
                "eventCode", eventCode == null ? "" : eventCode,
                "estimatedRows", estimatedRows)));
        row.setRowLimit(rowLimit);
        row.setStatus("QUEUED");
        row.setCreatedBy(scopedRequest.createdBy());
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(row.getCreatedAt());
        exportJobMapper.insert(row);
        return toExportView(row);
    }

    public ExportJobView exportStatus(Long tenantId, Long exportId) {
        Long scopedTenantId = guard.requireTenantId(tenantId);
        if (exportId == null || exportId <= 0) {
            throw new IllegalArgumentException("exportId is required");
        }
        requireExportJobMapper();

        AnalyticsExportJobDO row = exportJobMapper.selectById(exportId);
        if (row == null || !scopedTenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("export job not found");
        }
        return toExportView(row);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 as string 生成的文本或业务键。
     */
    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 as long 计算得到的数量、金额或指标值。
     */
    private static long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String requireKey(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 128) {
            throw new IllegalArgumentException(field + " cannot exceed 128 characters");
        }
        return trimmed;
    }

    private AlertPreviewRequest requireAlertPreviewRequest(AlertPreviewRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("alert preview request is required");
        }
        return new AlertPreviewRequest(
                requireKey("ruleKey", request.ruleKey()),
                request.eventCode(),
                request.startDate(),
                request.endDate());
    }

    private ExportRequest requireExportRequest(ExportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("export request is required");
        }
        return new ExportRequest(
                requireKey("reportType", request.reportType()).toUpperCase(Locale.ROOT),
                request.startDate(),
                request.endDate(),
                request.eventCode(),
                request.rowLimit(),
                request.createdBy());
    }

    private int normalizeRowLimit(Integer rowLimit) {
        int normalized = rowLimit == null || rowLimit < 1 ? maxExportRows : rowLimit;
        return Math.min(normalized, maxExportRows);
    }

    private List<Map<String, Object>> readSteps(String stepsJson) {
        if (stepsJson == null || stepsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(stepsJson, STEP_LIST_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("funnel steps_json is not valid JSON", e);
        }
    }

    private long thresholdValue(String thresholdJson) {
        if (thresholdJson == null || thresholdJson.isBlank()) {
            return 0L;
        }
        try {
            Map<String, Object> values = objectMapper.readValue(thresholdJson, MAP_TYPE);
            Object value = values.getOrDefault("countGte", values.get("threshold"));
            return Math.max(0L, asLong(value));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("alert threshold_json is not valid JSON", e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("analytics export query is not valid JSON", e);
        }
    }

    private ExportJobView toExportView(AnalyticsExportJobDO row) {
        return new ExportJobView(
                row.getId(),
                row.getReportType(),
                row.getRowLimit() == null ? 0 : row.getRowLimit(),
                row.getStatus(),
                row.getFileUrl(),
                row.getErrorMessage(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private void requireFunnelMapper() {
        if (funnelDefinitionMapper == null) {
            throw new IllegalStateException("analytics funnel mapper is not configured");
        }
    }

    private void requireAlertRuleMapper() {
        if (alertRuleMapper == null) {
            throw new IllegalStateException("analytics alert rule mapper is not configured");
        }
    }

    private void requireExportJobMapper() {
        if (exportJobMapper == null) {
            throw new IllegalStateException("analytics export job mapper is not configured");
        }
    }

    /**
     * EventCountRow 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record EventCountRow(String eventCode, long count) {
    }

    /**
     * EventTotal 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record EventTotal(long count) {
    }

    /**
     * UserTimelineRow 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record UserTimelineRow(String eventCode, String eventTime) {
    }

    /**
     * AttributeDistributionRow 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AttributeDistributionRow(String value, long count) {
    }

    public record FunnelResult(String funnelKey,
                               int version,
                               String name,
                               String startDate,
                               String endDate,
                               List<Map<String, Object>> steps) {
    }

    public record AlertPreviewRequest(String ruleKey,
                                      String eventCode,
                                      String startDate,
                                      String endDate) {
    }

    public record AlertPreviewResult(String ruleKey,
                                     String name,
                                     String eventCode,
                                     String startDate,
                                     String endDate,
                                     long count,
                                     long threshold,
                                     boolean triggered) {
    }

    public record ExportRequest(String reportType,
                                String startDate,
                                String endDate,
                                String eventCode,
                                Integer rowLimit,
                                String createdBy) {
    }

    public record ExportJobView(Long id,
                                String reportType,
                                int rowLimit,
                                String status,
                                String fileUrl,
                                String errorMessage,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt) {
    }
}
