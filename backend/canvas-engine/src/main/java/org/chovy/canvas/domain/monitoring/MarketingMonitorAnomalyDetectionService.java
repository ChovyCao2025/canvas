package org.chovy.canvas.domain.monitoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAlertDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAnomalyEventDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorAnomalyRuleDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorSourceDO;
import org.chovy.canvas.dal.dataobject.MarketingMonitorTrendSnapshotDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorAlertMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAnomalyEventMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorAnomalyRuleMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorSourceMapper;
import org.chovy.canvas.dal.mapper.MarketingMonitorTrendSnapshotMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * MarketingMonitorAnomalyDetectionService 编排 domain.monitoring 场景的领域业务规则。
 */
@Service
public class MarketingMonitorAnomalyDetectionService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
    private static final BigDecimal ROBUST_Z_FACTOR = new BigDecimal("0.674500");
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final MarketingMonitorAnomalyRuleMapper ruleMapper;
    private final MarketingMonitorAnomalyEventMapper eventMapper;
    private final MarketingMonitorTrendSnapshotMapper trendMapper;
    private final MarketingMonitorSourceMapper sourceMapper;
    private final MarketingMonitorAlertMapper alertMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 创建 MarketingMonitorAnomalyDetectionService 实例并注入 domain.monitoring 场景依赖。
     * @param ruleMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param trendMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param sourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public MarketingMonitorAnomalyDetectionService(MarketingMonitorAnomalyRuleMapper ruleMapper,
                                                   MarketingMonitorAnomalyEventMapper eventMapper,
                                                   MarketingMonitorTrendSnapshotMapper trendMapper,
                                                   MarketingMonitorSourceMapper sourceMapper,
                                                   MarketingMonitorAlertMapper alertMapper,
                                                   ObjectMapper objectMapper) {
        this(ruleMapper, eventMapper, trendMapper, sourceMapper, alertMapper, objectMapper,
                Clock.systemDefaultZone());
    }

    /**
     * 执行 MarketingMonitorAnomalyDetectionService 流程，围绕 marketing monitor anomaly detection service 完成校验、计算或结果组装。
     *
     * @param ruleMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param trendMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param sourceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    MarketingMonitorAnomalyDetectionService(MarketingMonitorAnomalyRuleMapper ruleMapper,
                                            MarketingMonitorAnomalyEventMapper eventMapper,
                                            MarketingMonitorTrendSnapshotMapper trendMapper,
                                            MarketingMonitorSourceMapper sourceMapper,
                                            MarketingMonitorAlertMapper alertMapper,
                                            ObjectMapper objectMapper,
                                            Clock clock) {
        this.ruleMapper = ruleMapper;
        this.eventMapper = eventMapper;
        this.trendMapper = trendMapper;
        this.sourceMapper = sourceMapper;
        this.alertMapper = alertMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 创建或更新业务记录，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public MarketingMonitorAnomalyRuleView upsertRule(Long tenantId,
                                                      MarketingMonitorAnomalyRuleCommand command,
                                                      String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("monitoring anomaly rule command is required");
        }
        Long scopedTenantId = tenantId(tenantId);
        if (command.sourceId() != null) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            MarketingMonitorSourceDO source = sourceMapper.selectById(command.sourceId());
            if (source == null || !scopedTenantId.equals(source.getTenantId())) {
                throw new IllegalArgumentException("source does not belong to current tenant");
            }
        }
        String ruleKey = requiredKey(command.ruleKey(), "ruleKey");
        LocalDateTime changedAt = now();
        MarketingMonitorAnomalyRuleDO row = ruleMapper.selectOne(new LambdaQueryWrapper<MarketingMonitorAnomalyRuleDO>()
                .eq(MarketingMonitorAnomalyRuleDO::getTenantId, scopedTenantId)
                .eq(MarketingMonitorAnomalyRuleDO::getRuleKey, ruleKey)
                .last("LIMIT 1"));
        if (row == null) {
            row = new MarketingMonitorAnomalyRuleDO();
            row.setTenantId(scopedTenantId);
            row.setRuleKey(ruleKey);
            row.setCreatedBy(actor(actor));
            row.setCreatedAt(changedAt);
        }
        row.setDisplayName(defaultString(command.displayName(), ruleKey));
        row.setSourceId(command.sourceId());
        row.setMetricKey(metricKey(command.metricKey()));
        row.setBucketGrain(normalizeUpper(defaultString(command.bucketGrain(), "DAY"), "bucketGrain"));
        row.setBrandKey(defaultString(normalizeOptionalKey(command.brandKey()), ""));
        row.setCompetitorKey(defaultString(normalizeOptionalKey(command.competitorKey()), ""));
        row.setDirection(direction(command.direction()));
        row.setBaselineWindowBuckets(boundedPositive(command.baselineWindowBuckets(), 14, 365));
        row.setMinBaselineBuckets(boundedPositive(command.minBaselineBuckets(), 5, row.getBaselineWindowBuckets()));
        row.setThresholdMultiplier(decimal(command.thresholdMultiplier(), new BigDecimal("3.0000"), 4));
        row.setMinDelta(decimal(command.minDelta(), new BigDecimal("1.000000"), 6));
        row.setEnabled(Boolean.FALSE.equals(command.enabled()) ? 0 : 1);
        row.setMetadataJson(json(command.metadata()));
        row.setUpdatedAt(changedAt);
        if (row.getId() == null) {
            ruleMapper.insert(row);
        } else {
            ruleMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toRuleView(row);
    }

    /**
     * 执行业务操作 detect，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param command 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public MarketingMonitorAnomalyDetectionView detect(Long tenantId,
                                                       MarketingMonitorAnomalyDetectionCommand command,
                                                       String actor) {
        if (command == null) {
            throw new IllegalArgumentException("monitoring anomaly detection command is required");
        }
        Long scopedTenantId = tenantId(tenantId);
        MarketingMonitorAnomalyRuleDO rule = requireRule(scopedTenantId, command.ruleId());
        LocalDateTime bucketStart = requiredTime(command.bucketStart(), "bucketStart");
        LocalDateTime bucketEnd = requiredTime(command.bucketEnd(), "bucketEnd");
        if (!bucketEnd.isAfter(bucketStart)) {
            throw new IllegalArgumentException("bucketEnd must be after bucketStart");
        }
        List<MarketingMonitorTrendSnapshotDO> snapshots = scopedSnapshots(scopedTenantId, rule);
        MarketingMonitorTrendSnapshotDO target = snapshots.stream()
                .filter(row -> bucketStart.equals(row.getBucketStart()))
                .filter(row -> bucketEnd.equals(row.getBucketEnd()))
                .findFirst()
                .orElse(null);
        if (target == null) {
            return emptyDetection(scopedTenantId, rule, "TARGET_BUCKET_NOT_FOUND", 0);
        }
        List<BigDecimal> baselineValues = snapshots.stream()
                .filter(row -> row.getBucketStart() != null && row.getBucketStart().isBefore(bucketStart))
                .sorted(Comparator.comparing(MarketingMonitorTrendSnapshotDO::getBucketStart).reversed())
                .limit(positive(rule.getBaselineWindowBuckets(), 14))
                .map(row -> metricValue(row, rule.getMetricKey()))
                .toList();
        int minBaseline = positive(rule.getMinBaselineBuckets(), 5);
        if (baselineValues.size() < minBaseline) {
            return emptyDetection(scopedTenantId, rule, "INSUFFICIENT_BASELINE", baselineValues.size());
        }

        BigDecimal actual = metricValue(target, rule.getMetricKey());
        BigDecimal median = median(baselineValues);
        BigDecimal mad = median(baselineValues.stream()
                .map(value -> value.subtract(median).abs())
                .toList());
        BigDecimal delta = actual.subtract(median).setScale(6, RoundingMode.HALF_UP);
        BigDecimal robustZ = mad.compareTo(ZERO) == 0
                ? ZERO
                : delta.multiply(ROBUST_Z_FACTOR).divide(mad, 6, RoundingMode.HALF_UP);
        String anomalyDirection = delta.compareTo(ZERO) >= 0 ? "SPIKE" : "DROP";
        boolean anomaly = isAnomaly(rule, delta, robustZ, mad, anomalyDirection);
        if (!anomaly) {
            return new MarketingMonitorAnomalyDetectionView(
                    scopedTenantId,
                    rule.getId(),
                    rule.getRuleKey(),
                    rule.getMetricKey(),
                    "NO_ANOMALY",
                    baselineValues.size(),
                    actual,
                    median,
                    mad,
                    robustZ,
                    delta,
                    null);
        }

        MarketingMonitorAnomalyEventDO event = upsertEvent(scopedTenantId, rule, target, actual, median, mad,
                /**
                 * 执行 severity 流程，围绕 severity 完成校验、计算或结果组装。
                 *
                 * @param rule rule 参数，用于 severity 流程中的校验、计算或对象转换。
                 * @param baselineValues baseline values 参数，用于 severity 流程中的校验、计算或对象转换。
                 * @param actor 操作人标识，用于审计和权限判断。
                 * @return 返回 severity 流程生成的业务结果。
                 */
                robustZ, delta, anomalyDirection, severity(rule, robustZ), baselineValues, actor);
        return new MarketingMonitorAnomalyDetectionView(
                scopedTenantId,
                rule.getId(),
                rule.getRuleKey(),
                rule.getMetricKey(),
                "ANOMALY_DETECTED",
                baselineValues.size(),
                actual,
                median,
                mad,
                robustZ,
                delta,
                toEventView(event));
    }

    /**
     * 执行业务操作 events，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param query query 参数，用于 events 流程中的校验、计算或对象转换。
     * @return 返回按租户、状态和数量限制过滤后的视图列表；无数据时返回空列表
     */
    public List<MarketingMonitorAnomalyEventView> events(Long tenantId, MarketingMonitorAnomalyEventQuery query) {
        // 准备本次流程的上下文、默认值和中间结果。
        Long scopedTenantId = tenantId(tenantId);
        MarketingMonitorAnomalyEventQuery effectiveQuery = query == null
                ? new MarketingMonitorAnomalyEventQuery(null, null, 50)
                : query;
        String status = normalizeOptionalUpper(effectiveQuery.status());
        int limit = boundedLimit(effectiveQuery.limit());
        // 访问持久化数据，读取现有配置或写入本次变更。
        return safeList(eventMapper.selectList(new LambdaQueryWrapper<MarketingMonitorAnomalyEventDO>()
                .eq(MarketingMonitorAnomalyEventDO::getTenantId, scopedTenantId)
                .eq(effectiveQuery.ruleId() != null, MarketingMonitorAnomalyEventDO::getRuleId,
                        effectiveQuery.ruleId())
                .eq(status != null, MarketingMonitorAnomalyEventDO::getStatus, status)
                .orderByDesc(MarketingMonitorAnomalyEventDO::getBucketStart)
                // 遍历候选记录并转换为前端或服务层需要的视图。
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> effectiveQuery.ruleId() == null || Objects.equals(effectiveQuery.ruleId(), row.getRuleId()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .limit(limit)
                .map(this::toEventView)
                .toList();
    }

    /**
     * 关闭或解析业务异常，作为营销监控的服务入口。
     * <p>调用方必须传入租户上下文或租户 ID，方法内的查询、写入和治理判断都限制在该租户范围内。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param tenantId 租户 ID，所有查询和写入都限定在该租户数据范围内
     * @param eventId 目标业务记录 ID，需与租户边界匹配
     * @param actor 操作人标识，用于审计字段、状态流转记录或治理追踪
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public MarketingMonitorAnomalyEventView resolveEvent(Long tenantId, Long eventId, String actor) {
        Long scopedTenantId = tenantId(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MarketingMonitorAnomalyEventDO row = eventMapper.selectById(requiredId(eventId, "eventId"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null || !scopedTenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("anomaly event is not found");
        }
        LocalDateTime resolvedAt = now();
        row.setStatus("RESOLVED");
        row.setResolvedBy(actor(actor));
        row.setResolvedAt(resolvedAt);
        row.setUpdatedAt(resolvedAt);
        eventMapper.updateById(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toEventView(row);
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param ruleId 业务对象 ID，用于定位具体记录。
     * @return 返回 requireRule 流程生成的业务结果。
     */
    private MarketingMonitorAnomalyRuleDO requireRule(Long tenantId, Long ruleId) {
        MarketingMonitorAnomalyRuleDO rule = ruleMapper.selectById(requiredId(ruleId, "ruleId"));
        if (rule == null || !tenantId.equals(rule.getTenantId())) {
            throw new IllegalArgumentException("anomaly rule is not found");
        }
        if (!Integer.valueOf(1).equals(rule.getEnabled())) {
            throw new IllegalStateException("anomaly rule is disabled");
        }
        return rule;
    }

    /**
     * 执行 scopedSnapshots 流程，围绕 scoped snapshots 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param rule rule 参数，用于 scopedSnapshots 流程中的校验、计算或对象转换。
     * @return 返回 scoped snapshots 汇总后的集合、分页或映射视图。
     */
    private List<MarketingMonitorTrendSnapshotDO> scopedSnapshots(Long tenantId, MarketingMonitorAnomalyRuleDO rule) {
        String brandKey = defaultString(normalizeOptionalKey(rule.getBrandKey()), "");
        String competitorKey = defaultString(normalizeOptionalKey(rule.getCompetitorKey()), "");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(trendMapper.selectList(new LambdaQueryWrapper<MarketingMonitorTrendSnapshotDO>()
                .eq(MarketingMonitorTrendSnapshotDO::getTenantId, tenantId)
                .eq(rule.getSourceId() != null, MarketingMonitorTrendSnapshotDO::getSourceId, rule.getSourceId())
                .eq(MarketingMonitorTrendSnapshotDO::getBucketGrain, rule.getBucketGrain())
                .eq(MarketingMonitorTrendSnapshotDO::getBrandKey, brandKey)
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .eq(MarketingMonitorTrendSnapshotDO::getCompetitorKey, competitorKey))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .filter(row -> rule.getSourceId() == null || Objects.equals(rule.getSourceId(), row.getSourceId()))
                .filter(row -> Objects.equals(rule.getBucketGrain(), row.getBucketGrain()))
                .filter(row -> brandKey.equals(defaultString(normalizeOptionalKey(row.getBrandKey()), "")))
                .filter(row -> competitorKey.equals(defaultString(normalizeOptionalKey(row.getCompetitorKey()), "")))
                .toList();
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param rule rule 参数，用于 upsertEvent 流程中的校验、计算或对象转换。
     * @param target target 参数，用于 upsertEvent 流程中的校验、计算或对象转换。
     * @param actual actual 参数，用于 upsertEvent 流程中的校验、计算或对象转换。
     * @param median median 参数，用于 upsertEvent 流程中的校验、计算或对象转换。
     * @param mad mad 参数，用于 upsertEvent 流程中的校验、计算或对象转换。
     * @param robustZ robust z 参数，用于 upsertEvent 流程中的校验、计算或对象转换。
     * @param delta delta 参数，用于 upsertEvent 流程中的校验、计算或对象转换。
     * @param direction direction 参数，用于 upsertEvent 流程中的校验、计算或对象转换。
     * @param severity severity 参数，用于 upsertEvent 流程中的校验、计算或对象转换。
     * @param baselineValues baseline values 参数，用于 upsertEvent 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回流程执行后的业务结果。
     */
    private MarketingMonitorAnomalyEventDO upsertEvent(Long tenantId,
                                                       MarketingMonitorAnomalyRuleDO rule,
                                                       MarketingMonitorTrendSnapshotDO target,
                                                       BigDecimal actual,
                                                       BigDecimal median,
                                                       BigDecimal mad,
                                                       BigDecimal robustZ,
                                                       BigDecimal delta,
                                                       String direction,
                                                       String severity,
                                                       List<BigDecimal> baselineValues,
                                                       String actor) {
        // 准备本次处理所需的上下文和中间变量。
        LocalDateTime changedAt = now();
        Long sourceId = target.getSourceId() == null ? 0L : target.getSourceId();
        String brandKey = defaultString(normalizeOptionalKey(target.getBrandKey()), "");
        String competitorKey = defaultString(normalizeOptionalKey(target.getCompetitorKey()), "");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MarketingMonitorAnomalyEventDO row = eventMapper.selectOne(new LambdaQueryWrapper<MarketingMonitorAnomalyEventDO>()
                .eq(MarketingMonitorAnomalyEventDO::getTenantId, tenantId)
                .eq(MarketingMonitorAnomalyEventDO::getRuleId, rule.getId())
                .eq(MarketingMonitorAnomalyEventDO::getMetricKey, rule.getMetricKey())
                .eq(MarketingMonitorAnomalyEventDO::getSourceId, sourceId)
                .eq(MarketingMonitorAnomalyEventDO::getBrandKey, brandKey)
                .eq(MarketingMonitorAnomalyEventDO::getCompetitorKey, competitorKey)
                .eq(MarketingMonitorAnomalyEventDO::getBucketStart, target.getBucketStart())
                .last("LIMIT 1"));
        boolean created = row == null;
        if (row == null) {
            row = new MarketingMonitorAnomalyEventDO();
            row.setTenantId(tenantId);
            row.setRuleId(rule.getId());
            row.setRuleKey(rule.getRuleKey());
            row.setSourceId(sourceId);
            row.setMetricKey(rule.getMetricKey());
            row.setBucketStart(target.getBucketStart());
            row.setBrandKey(brandKey);
            row.setCompetitorKey(competitorKey);
            row.setStatus("OPEN");
            row.setCreatedBy(actor(actor));
            row.setCreatedAt(changedAt);
        }
        row.setSourceKey(target.getSourceKey());
        row.setBucketGrain(target.getBucketGrain());
        row.setBucketEnd(target.getBucketEnd());
        row.setActualValue(actual);
        row.setBaselineMedian(median);
        row.setBaselineMad(mad);
        row.setRobustZScore(robustZ);
        row.setDeltaValue(delta);
        row.setDirection(direction);
        row.setSeverity(severity);
        row.setEvidenceJson(json(evidence(rule, target, baselineValues)));
        row.setUpdatedAt(changedAt);
        if (created) {
            eventMapper.insert(row);
            insertAlert(tenantId, rule, row, actor, changedAt);
        } else {
            eventMapper.updateById(row);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return row;
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param rule rule 参数，用于 insertAlert 流程中的校验、计算或对象转换。
     * @param event event 参数，用于 insertAlert 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param createdAt 时间参数，用于计算窗口、过期或审计时间。
     */
    private void insertAlert(Long tenantId,
                             MarketingMonitorAnomalyRuleDO rule,
                             MarketingMonitorAnomalyEventDO event,
                             String actor,
                             LocalDateTime createdAt) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (alertMapper == null) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        MarketingMonitorAlertDO alert = new MarketingMonitorAlertDO();
        alert.setTenantId(tenantId);
        alert.setAlertType("ANOMALY_DETECTED");
        alert.setSeverity(event.getSeverity());
        alert.setStatus("OPEN");
        alert.setScopeKey(defaultString(normalizeOptionalKey(event.getBrandKey()), rule.getRuleKey()));
        alert.setTitle("Monitoring anomaly detected: " + rule.getDisplayName());
        alert.setReason(event.getMetricKey() + " " + event.getDirection().toLowerCase(Locale.ROOT)
                + " detected with robust z-score " + event.getRobustZScore());
        alert.setItemCount(1);
        alert.setWindowStart(event.getBucketStart());
        alert.setWindowEnd(event.getBucketEnd());
        alert.setMetadataJson(json(Map.of(
                "anomalyEventId", event.getId(),
                "ruleId", event.getRuleId(),
                "ruleKey", event.getRuleKey(),
                "metricKey", event.getMetricKey(),
                "actualValue", event.getActualValue(),
                "baselineMedian", event.getBaselineMedian(),
                "robustZScore", event.getRobustZScore())));
        alert.setCreatedBy(actor(actor));
        alert.setCreatedAt(createdAt);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        alert.setUpdatedAt(createdAt);
        alertMapper.insert(alert);
    }

    /**
     * 执行 evidence 流程，围绕 evidence 完成校验、计算或结果组装。
     *
     * @param rule rule 参数，用于 evidence 流程中的校验、计算或对象转换。
     * @param target target 参数，用于 evidence 流程中的校验、计算或对象转换。
     * @param baselineValues baseline values 参数，用于 evidence 流程中的校验、计算或对象转换。
     * @return 返回 evidence 流程生成的业务结果。
     */
    private Map<String, Object> evidence(MarketingMonitorAnomalyRuleDO rule,
                                         MarketingMonitorTrendSnapshotDO target,
                                         List<BigDecimal> baselineValues) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("ruleKey", rule.getRuleKey());
        evidence.put("metricKey", rule.getMetricKey());
        evidence.put("bucketStart", target.getBucketStart() == null ? null : target.getBucketStart().toString());
        evidence.put("bucketEnd", target.getBucketEnd() == null ? null : target.getBucketEnd().toString());
        evidence.put("baselineValues", baselineValues);
        evidence.put("thresholdMultiplier", rule.getThresholdMultiplier());
        evidence.put("minDelta", rule.getMinDelta());
        return evidence;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param rule rule 参数，用于 isAnomaly 流程中的校验、计算或对象转换。
     * @param delta delta 参数，用于 isAnomaly 流程中的校验、计算或对象转换。
     * @param robustZ robust z 参数，用于 isAnomaly 流程中的校验、计算或对象转换。
     * @param mad mad 参数，用于 isAnomaly 流程中的校验、计算或对象转换。
     * @param anomalyDirection anomaly direction 参数，用于 isAnomaly 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean isAnomaly(MarketingMonitorAnomalyRuleDO rule,
                              BigDecimal delta,
                              BigDecimal robustZ,
                              BigDecimal mad,
                              String anomalyDirection) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!direction(rule.getDirection()).equals("BOTH") && !direction(rule.getDirection()).equals(anomalyDirection)) {
            return false;
        }
        BigDecimal minDelta = decimal(rule.getMinDelta(), BigDecimal.ONE, 6);
        if (delta.abs().compareTo(minDelta) < 0) {
            return false;
        }
        if (mad.compareTo(ZERO) == 0) {
            return true;
        }
        BigDecimal threshold = decimal(rule.getThresholdMultiplier(), new BigDecimal("3.0000"), 4);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return robustZ.abs().compareTo(threshold) >= 0;
    }

    /**
     * 执行 severity 流程，围绕 severity 完成校验、计算或结果组装。
     *
     * @param rule rule 参数，用于 severity 流程中的校验、计算或对象转换。
     * @param robustZ robust z 参数，用于 severity 流程中的校验、计算或对象转换。
     * @return 返回 severity 生成的文本或业务键。
     */
    private String severity(MarketingMonitorAnomalyRuleDO rule, BigDecimal robustZ) {
        BigDecimal threshold = decimal(rule.getThresholdMultiplier(), new BigDecimal("3.0000"), 4);
        BigDecimal score = robustZ.abs();
        if (score.compareTo(threshold.multiply(new BigDecimal("2.0000"))) >= 0) {
            return "CRITICAL";
        }
        if (score.compareTo(threshold.multiply(new BigDecimal("1.5000"))) >= 0) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    /**
     * 执行 emptyDetection 流程，围绕 empty detection 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param rule rule 参数，用于 emptyDetection 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param baselineBucketCount baseline bucket count 参数，用于 emptyDetection 流程中的校验、计算或对象转换。
     * @return 返回 emptyDetection 流程生成的业务结果。
     */
    private MarketingMonitorAnomalyDetectionView emptyDetection(Long tenantId,
                                                                MarketingMonitorAnomalyRuleDO rule,
                                                                String status,
                                                                int baselineBucketCount) {
        return new MarketingMonitorAnomalyDetectionView(
                tenantId,
                rule.getId(),
                rule.getRuleKey(),
                rule.getMetricKey(),
                status,
                baselineBucketCount,
                ZERO,
                ZERO,
                ZERO,
                ZERO,
                ZERO,
                null);
    }

    /**
     * 执行 metricValue 流程，围绕 metric value 完成校验、计算或结果组装。
     *
     * @param snapshot snapshot 参数，用于 metricValue 流程中的校验、计算或对象转换。
     * @param metricKey 业务键，用于在同一租户下定位资源。
     * @return 返回 metric value 计算得到的数量、金额或指标值。
     */
    private BigDecimal metricValue(MarketingMonitorTrendSnapshotDO snapshot, String metricKey) {
        return switch (metricKey(metricKey)) {
            case "MENTION_COUNT" -> decimal(snapshot.getMentionCount());
            case "NEGATIVE_COUNT" -> decimal(snapshot.getNegativeCount());
            case "COMPETITOR_COUNT" -> decimal(snapshot.getCompetitorCount());
            case "ALERT_COUNT" -> decimal(snapshot.getAlertCount());
            case "AVG_SENTIMENT_SCORE" -> decimal(snapshot.getAvgSentimentScore(), ZERO, 6);
            default -> ZERO;
        };
    }

    /**
     * 执行 median 流程，围绕 median 完成校验、计算或结果组装。
     *
     * @param values values 参数，用于 median 流程中的校验、计算或对象转换。
     * @return 返回 median 计算得到的数量、金额或指标值。
     */
    private BigDecimal median(List<BigDecimal> values) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (values == null || values.isEmpty()) {
            return ZERO;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<BigDecimal> sorted = values.stream()
                .map(value -> decimal(value, ZERO, 6))
                .sorted()
                .toList();
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle).setScale(6, RoundingMode.HALF_UP);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return sorted.get(middle - 1).add(sorted.get(middle))
                .divide(new BigDecimal("2.000000"), 6, RoundingMode.HALF_UP);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingMonitorAnomalyRuleView toRuleView(MarketingMonitorAnomalyRuleDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingMonitorAnomalyRuleView(
                row.getId(),
                row.getTenantId(),
                row.getRuleKey(),
                row.getDisplayName(),
                row.getSourceId(),
                row.getMetricKey(),
                row.getBucketGrain(),
                row.getBrandKey(),
                row.getCompetitorKey(),
                row.getDirection(),
                row.getBaselineWindowBuckets(),
                row.getMinBaselineBuckets(),
                row.getThresholdMultiplier(),
                row.getMinDelta(),
                Integer.valueOf(1).equals(row.getEnabled()),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getCreatedAt(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private MarketingMonitorAnomalyEventView toEventView(MarketingMonitorAnomalyEventDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new MarketingMonitorAnomalyEventView(
                row.getId(),
                row.getTenantId(),
                row.getRuleId(),
                row.getRuleKey(),
                row.getSourceId(),
                row.getSourceKey(),
                row.getMetricKey(),
                row.getBucketGrain(),
                row.getBucketStart(),
                row.getBucketEnd(),
                row.getBrandKey(),
                row.getCompetitorKey(),
                decimal(row.getActualValue(), ZERO, 6),
                decimal(row.getBaselineMedian(), ZERO, 6),
                decimal(row.getBaselineMad(), ZERO, 6),
                decimal(row.getRobustZScore(), ZERO, 6),
                decimal(row.getDeltaValue(), ZERO, 6),
                row.getDirection(),
                row.getSeverity(),
                row.getStatus(),
                map(row.getEvidenceJson()),
                row.getCreatedBy(),
                row.getResolvedBy(),
                row.getResolvedAt(),
                row.getCreatedAt(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 执行 metricKey 流程，围绕 metric key 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 metric key 生成的文本或业务键。
     */
    private String metricKey(String value) {
        String metric = normalizeUpper(value, "metricKey");
        if (!List.of("MENTION_COUNT", "NEGATIVE_COUNT", "COMPETITOR_COUNT", "ALERT_COUNT", "AVG_SENTIMENT_SCORE")
                .contains(metric)) {
            throw new IllegalArgumentException("unsupported anomaly metric: " + metric);
        }
        return metric;
    }

    /**
     * 执行 direction 流程，围绕 direction 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 direction 生成的文本或业务键。
     */
    private String direction(String value) {
        String direction = normalizeUpper(defaultString(value, "BOTH"), "direction");
        if (!List.of("SPIKE", "DROP", "BOTH").contains(direction)) {
            throw new IllegalArgumentException("unsupported anomaly direction: " + direction);
        }
        return direction;
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(Long tenantId) {
        if (tenantId == null || tenantId < 0) {
            return 0L;
        }
        return tenantId;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required id 计算得到的数量、金额或指标值。
     */
    private Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 requiredTime 流程生成的业务结果。
     */
    private LocalDateTime requiredTime(LocalDateTime value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required key 生成的文本或业务键。
     */
    private String requiredKey(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeUpper(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOptionalUpper(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOptionalKey(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    /**
     * 解析操作人标识。
     *
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 actor 生成的文本或业务键。
     */
    private String actor(String actor) {
        return defaultString(actor, "system");
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 boundedPositive 流程中的校验、计算或对象转换。
     * @param max max 参数，用于 boundedPositive 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedPositive(Integer value, int fallback, int max) {
        int effective = value == null || value <= 0 ? fallback : value;
        return Math.max(1, Math.min(effective, max));
    }

    /**
     * 执行 positive 流程，围绕 positive 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 positive 流程中的校验、计算或对象转换。
     * @return 返回 positive 计算得到的数量、金额或指标值。
     */
    private int positive(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 50;
        }
        return Math.min(limit, 100);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private BigDecimal decimal(Integer value) {
        return BigDecimal.valueOf(value == null ? 0L : value.longValue()).setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 decimal 流程中的校验、计算或对象转换。
     * @param scale scale 参数，用于 decimal 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private BigDecimal decimal(BigDecimal value, BigDecimal fallback, int scale) {
        BigDecimal effective = value == null ? fallback : value;
        return effective.setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * 执行 now 流程，围绕 now 完成校验、计算或结果组装。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("monitoring anomaly metadata is not JSON serializable", ex);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> values = objectMapper.readValue(json, OBJECT_MAP);
            return values == null ? Map.of() : values;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }
}
