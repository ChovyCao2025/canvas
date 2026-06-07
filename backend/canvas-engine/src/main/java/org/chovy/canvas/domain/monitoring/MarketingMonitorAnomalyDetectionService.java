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

    public MarketingMonitorAnomalyRuleView upsertRule(Long tenantId,
                                                      MarketingMonitorAnomalyRuleCommand command,
                                                      String actor) {
        if (command == null) {
            throw new IllegalArgumentException("monitoring anomaly rule command is required");
        }
        Long scopedTenantId = tenantId(tenantId);
        if (command.sourceId() != null) {
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
        return toRuleView(row);
    }

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

    public List<MarketingMonitorAnomalyEventView> events(Long tenantId, MarketingMonitorAnomalyEventQuery query) {
        Long scopedTenantId = tenantId(tenantId);
        MarketingMonitorAnomalyEventQuery effectiveQuery = query == null
                ? new MarketingMonitorAnomalyEventQuery(null, null, 50)
                : query;
        String status = normalizeOptionalUpper(effectiveQuery.status());
        int limit = boundedLimit(effectiveQuery.limit());
        return safeList(eventMapper.selectList(new LambdaQueryWrapper<MarketingMonitorAnomalyEventDO>()
                .eq(MarketingMonitorAnomalyEventDO::getTenantId, scopedTenantId)
                .eq(effectiveQuery.ruleId() != null, MarketingMonitorAnomalyEventDO::getRuleId,
                        effectiveQuery.ruleId())
                .eq(status != null, MarketingMonitorAnomalyEventDO::getStatus, status)
                .orderByDesc(MarketingMonitorAnomalyEventDO::getBucketStart)
                .last("LIMIT " + limit))).stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> effectiveQuery.ruleId() == null || Objects.equals(effectiveQuery.ruleId(), row.getRuleId()))
                .filter(row -> status == null || status.equals(row.getStatus()))
                .limit(limit)
                .map(this::toEventView)
                .toList();
    }

    public MarketingMonitorAnomalyEventView resolveEvent(Long tenantId, Long eventId, String actor) {
        Long scopedTenantId = tenantId(tenantId);
        MarketingMonitorAnomalyEventDO row = eventMapper.selectById(requiredId(eventId, "eventId"));
        if (row == null || !scopedTenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("anomaly event is not found");
        }
        LocalDateTime resolvedAt = now();
        row.setStatus("RESOLVED");
        row.setResolvedBy(actor(actor));
        row.setResolvedAt(resolvedAt);
        row.setUpdatedAt(resolvedAt);
        eventMapper.updateById(row);
        return toEventView(row);
    }

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

    private List<MarketingMonitorTrendSnapshotDO> scopedSnapshots(Long tenantId, MarketingMonitorAnomalyRuleDO rule) {
        String brandKey = defaultString(normalizeOptionalKey(rule.getBrandKey()), "");
        String competitorKey = defaultString(normalizeOptionalKey(rule.getCompetitorKey()), "");
        return safeList(trendMapper.selectList(new LambdaQueryWrapper<MarketingMonitorTrendSnapshotDO>()
                .eq(MarketingMonitorTrendSnapshotDO::getTenantId, tenantId)
                .eq(rule.getSourceId() != null, MarketingMonitorTrendSnapshotDO::getSourceId, rule.getSourceId())
                .eq(MarketingMonitorTrendSnapshotDO::getBucketGrain, rule.getBucketGrain())
                .eq(MarketingMonitorTrendSnapshotDO::getBrandKey, brandKey)
                .eq(MarketingMonitorTrendSnapshotDO::getCompetitorKey, competitorKey))).stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .filter(row -> rule.getSourceId() == null || Objects.equals(rule.getSourceId(), row.getSourceId()))
                .filter(row -> Objects.equals(rule.getBucketGrain(), row.getBucketGrain()))
                .filter(row -> brandKey.equals(defaultString(normalizeOptionalKey(row.getBrandKey()), "")))
                .filter(row -> competitorKey.equals(defaultString(normalizeOptionalKey(row.getCompetitorKey()), "")))
                .toList();
    }

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
        LocalDateTime changedAt = now();
        Long sourceId = target.getSourceId() == null ? 0L : target.getSourceId();
        String brandKey = defaultString(normalizeOptionalKey(target.getBrandKey()), "");
        String competitorKey = defaultString(normalizeOptionalKey(target.getCompetitorKey()), "");
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
        return row;
    }

    private void insertAlert(Long tenantId,
                             MarketingMonitorAnomalyRuleDO rule,
                             MarketingMonitorAnomalyEventDO event,
                             String actor,
                             LocalDateTime createdAt) {
        if (alertMapper == null) {
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
        alert.setUpdatedAt(createdAt);
        alertMapper.insert(alert);
    }

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

    private boolean isAnomaly(MarketingMonitorAnomalyRuleDO rule,
                              BigDecimal delta,
                              BigDecimal robustZ,
                              BigDecimal mad,
                              String anomalyDirection) {
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
        return robustZ.abs().compareTo(threshold) >= 0;
    }

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

    private BigDecimal median(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return ZERO;
        }
        List<BigDecimal> sorted = values.stream()
                .map(value -> decimal(value, ZERO, 6))
                .sorted()
                .toList();
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle).setScale(6, RoundingMode.HALF_UP);
        }
        return sorted.get(middle - 1).add(sorted.get(middle))
                .divide(new BigDecimal("2.000000"), 6, RoundingMode.HALF_UP);
    }

    private MarketingMonitorAnomalyRuleView toRuleView(MarketingMonitorAnomalyRuleDO row) {
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
                row.getUpdatedAt());
    }

    private MarketingMonitorAnomalyEventView toEventView(MarketingMonitorAnomalyEventDO row) {
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
                row.getUpdatedAt());
    }

    private String metricKey(String value) {
        String metric = normalizeUpper(value, "metricKey");
        if (!List.of("MENTION_COUNT", "NEGATIVE_COUNT", "COMPETITOR_COUNT", "ALERT_COUNT", "AVG_SENTIMENT_SCORE")
                .contains(metric)) {
            throw new IllegalArgumentException("unsupported anomaly metric: " + metric);
        }
        return metric;
    }

    private String direction(String value) {
        String direction = normalizeUpper(defaultString(value, "BOTH"), "direction");
        if (!List.of("SPIKE", "DROP", "BOTH").contains(direction)) {
            throw new IllegalArgumentException("unsupported anomaly direction: " + direction);
        }
        return direction;
    }

    private Long tenantId(Long tenantId) {
        if (tenantId == null || tenantId < 0) {
            return 0L;
        }
        return tenantId;
    }

    private Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private LocalDateTime requiredTime(LocalDateTime value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String requiredKey(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private String normalizeUpper(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalUpper(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalKey(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private String defaultString(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private String actor(String actor) {
        return defaultString(actor, "system");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int boundedPositive(Integer value, int fallback, int max) {
        int effective = value == null || value <= 0 ? fallback : value;
        return Math.max(1, Math.min(effective, max));
    }

    private int positive(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private int boundedLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 50;
        }
        return Math.min(limit, 100);
    }

    private BigDecimal decimal(Integer value) {
        return BigDecimal.valueOf(value == null ? 0L : value.longValue()).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal(BigDecimal value, BigDecimal fallback, int scale) {
        BigDecimal effective = value == null ? fallback : value;
        return effective.setScale(scale, RoundingMode.HALF_UP);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("monitoring anomaly metadata is not JSON serializable", ex);
        }
    }

    private Map<String, Object> map(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> values = objectMapper.readValue(json, OBJECT_MAP);
            return values == null ? Map.of() : values;
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }
}
