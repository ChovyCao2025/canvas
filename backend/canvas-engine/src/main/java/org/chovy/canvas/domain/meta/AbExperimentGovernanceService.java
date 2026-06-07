package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.AbExperimentGovernanceDecisionDO;
import org.chovy.canvas.dal.dataobject.AbExperimentMetricDO;
import org.chovy.canvas.dal.dataobject.AbExperimentMetricSnapshotDO;
import org.chovy.canvas.dal.mapper.AbExperimentGovernanceDecisionMapper;
import org.chovy.canvas.dal.mapper.AbExperimentMetricMapper;
import org.chovy.canvas.dal.mapper.AbExperimentMetricSnapshotMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class AbExperimentGovernanceService {

    private static final String ROLE_PRIMARY = "PRIMARY";
    private static final String ROLE_GUARDRAIL = "GUARDRAIL";
    private static final String DIRECTION_INCREASE = "INCREASE";
    private static final String DIRECTION_DECREASE = "DECREASE";
    private static final String STATUS_CONFIG_INVALID = "CONFIG_INVALID";
    private static final String STATUS_MISSING_SNAPSHOT = "MISSING_SNAPSHOT";
    private static final String STATUS_GUARDRAIL_BREACH = "GUARDRAIL_BREACH";
    private static final String STATUS_INSUFFICIENT_SAMPLE = "INSUFFICIENT_SAMPLE";
    private static final String STATUS_WINNER_CANDIDATE = "WINNER_CANDIDATE";
    private static final String STATUS_KEEP_RUNNING = "KEEP_RUNNING";
    private static final String WRITEBACK_NOT_READY = "NOT_READY";
    private static final String WRITEBACK_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String WRITEBACK_BLOCKED = "BLOCKED";
    private static final BigDecimal DEFAULT_MDE = new BigDecimal("0.05000000");
    private static final BigDecimal DEFAULT_GUARDRAIL_REGRESSION = new BigDecimal("0.01000000");
    private static final BigDecimal CONFIDENCE_THRESHOLD = new BigDecimal("0.950000");
    private static final int CONFIDENCE_SCALE = 6;

    private final AbExperimentMetricMapper metricMapper;
    private final AbExperimentMetricSnapshotMapper snapshotMapper;
    private final AbExperimentGovernanceDecisionMapper decisionMapper;
    private final Clock clock;

    @Autowired
    public AbExperimentGovernanceService(AbExperimentMetricMapper metricMapper,
                                         AbExperimentMetricSnapshotMapper snapshotMapper,
                                         AbExperimentGovernanceDecisionMapper decisionMapper) {
        this(metricMapper, snapshotMapper, decisionMapper, Clock.systemDefaultZone());
    }

    AbExperimentGovernanceService(AbExperimentMetricMapper metricMapper,
                                  AbExperimentMetricSnapshotMapper snapshotMapper,
                                  AbExperimentGovernanceDecisionMapper decisionMapper,
                                  Clock clock) {
        this.metricMapper = metricMapper;
        this.snapshotMapper = snapshotMapper;
        this.decisionMapper = decisionMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public Evaluation evaluate(Long experimentId, String controlVariantKey) {
        Long id = requireId(experimentId);
        String control = variantKey(controlVariantKey);
        LocalDateTime evaluatedAt = now();
        List<String> reasons = new ArrayList<>();
        List<AbExperimentMetricDO> metrics = metrics(id);
        List<AbExperimentMetricDO> primaryMetrics = metrics.stream()
                .filter(metric -> ROLE_PRIMARY.equals(role(metric)))
                .toList();
        if (primaryMetrics.size() != 1) {
            reasons.add("exactly one PRIMARY metric is required before experiment governance evaluation");
            return persist(new Evaluation(id, STATUS_CONFIG_INVALID, null, null, null,
                    0L, WRITEBACK_NOT_READY, List.copyOf(reasons), List.of()), evaluatedAt);
        }
        AbExperimentMetricDO primaryMetric = primaryMetrics.getFirst();
        Map<String, Map<String, AbExperimentMetricSnapshotDO>> snapshots = latestSnapshots(id);
        AbExperimentMetricSnapshotDO controlPrimary = snapshot(snapshots, primaryMetric.getMetricKey(), control);
        if (controlPrimary == null) {
            reasons.add("control variant " + control + " is missing primary metric " + primaryMetric.getMetricKey());
            return persist(new Evaluation(id, STATUS_MISSING_SNAPSHOT, null, primaryMetric.getMetricKey(), null,
                    requiredSample(primaryMetric, BigDecimal.ZERO), WRITEBACK_NOT_READY,
                    List.copyOf(reasons), List.of()), evaluatedAt);
        }
        List<AbExperimentMetricSnapshotDO> treatmentPrimarySnapshots = snapshotVariants(
                        snapshots, primaryMetric.getMetricKey()).entrySet().stream()
                .filter(entry -> !control.equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .toList();
        if (treatmentPrimarySnapshots.isEmpty()) {
            reasons.add("no treatment variant has primary metric " + primaryMetric.getMetricKey());
            return persist(new Evaluation(id, STATUS_MISSING_SNAPSHOT, null, primaryMetric.getMetricKey(), null,
                    requiredSample(primaryMetric, value(controlPrimary)), WRITEBACK_NOT_READY,
                    List.copyOf(reasons), List.of()), evaluatedAt);
        }

        AbExperimentMetricSnapshotDO bestPrimary = bestTreatment(primaryMetric, treatmentPrimarySnapshots);
        String candidateVariant = bestPrimary.getVariantKey();
        long requiredSample = requiredSample(primaryMetric, value(controlPrimary));
        BigDecimal confidence = confidence(primaryMetric, controlPrimary, bestPrimary);
        List<MetricOutcome> outcomes = new ArrayList<>();
        outcomes.add(outcome(primaryMetric, controlPrimary, bestPrimary, confidence, requiredSample));

        List<String> guardrailBreaches = guardrailBreaches(metrics, snapshots, control, candidateVariant, outcomes);
        if (!guardrailBreaches.isEmpty()) {
            reasons.addAll(guardrailBreaches);
            return persist(new Evaluation(id, STATUS_GUARDRAIL_BREACH, null, primaryMetric.getMetricKey(),
                    confidence, requiredSample, WRITEBACK_BLOCKED, List.copyOf(reasons), List.copyOf(outcomes)),
                    evaluatedAt);
        }

        if (sampleSize(controlPrimary) < requiredSample || sampleSize(bestPrimary) < requiredSample) {
            reasons.add("primary metric " + primaryMetric.getMetricKey()
                    + " requires at least " + requiredSample + " samples per variant before winner review");
            return persist(new Evaluation(id, STATUS_INSUFFICIENT_SAMPLE, null, primaryMetric.getMetricKey(),
                    confidence, requiredSample, WRITEBACK_NOT_READY, List.copyOf(reasons), List.copyOf(outcomes)),
                    evaluatedAt);
        }

        if (isPositiveLift(primaryMetric, controlPrimary, bestPrimary)
                && confidence.compareTo(CONFIDENCE_THRESHOLD) >= 0) {
            reasons.add("winner candidate " + candidateVariant
                    + " requires manual review before audience or tag writeback");
            return persist(new Evaluation(id, STATUS_WINNER_CANDIDATE, candidateVariant, primaryMetric.getMetricKey(),
                    confidence, requiredSample, WRITEBACK_PENDING_REVIEW, List.copyOf(reasons), List.copyOf(outcomes)),
                    evaluatedAt);
        }

        reasons.add("experiment should keep running until confidence and guardrails pass");
        return persist(new Evaluation(id, STATUS_KEEP_RUNNING, null, primaryMetric.getMetricKey(),
                confidence, requiredSample, WRITEBACK_NOT_READY, List.copyOf(reasons), List.copyOf(outcomes)),
                evaluatedAt);
    }

    private List<AbExperimentMetricDO> metrics(Long experimentId) {
        List<AbExperimentMetricDO> rows = metricMapper.selectList(new LambdaQueryWrapper<AbExperimentMetricDO>()
                .eq(AbExperimentMetricDO::getExperimentId, experimentId)
                .eq(AbExperimentMetricDO::getEnabled, 1)
                .orderByAsc(AbExperimentMetricDO::getMetricRole)
                .orderByAsc(AbExperimentMetricDO::getId));
        return rows == null ? List.of() : rows;
    }

    private Map<String, Map<String, AbExperimentMetricSnapshotDO>> latestSnapshots(Long experimentId) {
        List<AbExperimentMetricSnapshotDO> rows = snapshotMapper.selectList(
                new LambdaQueryWrapper<AbExperimentMetricSnapshotDO>()
                        .eq(AbExperimentMetricSnapshotDO::getExperimentId, experimentId)
                        .orderByDesc(AbExperimentMetricSnapshotDO::getObservedAt)
                        .orderByDesc(AbExperimentMetricSnapshotDO::getId));
        Map<String, Map<String, AbExperimentMetricSnapshotDO>> snapshots = new LinkedHashMap<>();
        for (AbExperimentMetricSnapshotDO row : rows == null ? List.<AbExperimentMetricSnapshotDO>of() : rows) {
            if (row == null || blank(row.getMetricKey()) || blank(row.getVariantKey())) {
                continue;
            }
            snapshots.computeIfAbsent(row.getMetricKey(), ignored -> new LinkedHashMap<>())
                    .putIfAbsent(row.getVariantKey(), row);
        }
        return snapshots;
    }

    private List<String> guardrailBreaches(List<AbExperimentMetricDO> metrics,
                                           Map<String, Map<String, AbExperimentMetricSnapshotDO>> snapshots,
                                           String controlVariant,
                                           String candidateVariant,
                                           List<MetricOutcome> outcomes) {
        List<String> breaches = new ArrayList<>();
        for (AbExperimentMetricDO guardrail : metrics.stream()
                .filter(metric -> ROLE_GUARDRAIL.equals(role(metric)))
                .toList()) {
            AbExperimentMetricSnapshotDO control = snapshot(snapshots, guardrail.getMetricKey(), controlVariant);
            AbExperimentMetricSnapshotDO candidate = snapshot(snapshots, guardrail.getMetricKey(), candidateVariant);
            if (control == null || candidate == null) {
                breaches.add("guardrail metric " + guardrail.getMetricKey()
                        + " is missing snapshots for " + controlVariant + " or " + candidateVariant);
                continue;
            }
            BigDecimal confidence = confidence(guardrail, control, candidate);
            outcomes.add(outcome(guardrail, control, candidate, confidence, 0L));
            BigDecimal allowedRegression = positiveOrDefault(
                    guardrail.getGuardrailMaxRegression(), DEFAULT_GUARDRAIL_REGRESSION);
            BigDecimal regression = regression(guardrail, control, candidate);
            if (regression.compareTo(allowedRegression) > 0) {
                breaches.add("guardrail " + guardrail.getMetricKey() + " for variant " + candidateVariant
                        + " breached allowed regression " + allowedRegression);
            }
        }
        return breaches;
    }

    private MetricOutcome outcome(AbExperimentMetricDO metric,
                                  AbExperimentMetricSnapshotDO control,
                                  AbExperimentMetricSnapshotDO treatment,
                                  BigDecimal confidence,
                                  long requiredSample) {
        return new MetricOutcome(
                metric.getMetricKey(),
                role(metric),
                direction(metric),
                control.getVariantKey(),
                treatment.getVariantKey(),
                value(control),
                value(treatment),
                value(treatment).subtract(value(control)).setScale(8, RoundingMode.HALF_UP),
                confidence,
                sampleSize(control),
                sampleSize(treatment),
                requiredSample);
    }

    private Evaluation persist(Evaluation evaluation, LocalDateTime evaluatedAt) {
        AbExperimentGovernanceDecisionDO row = new AbExperimentGovernanceDecisionDO();
        row.setExperimentId(evaluation.experimentId());
        row.setStatus(evaluation.status());
        row.setWinnerVariantKey(evaluation.winnerVariantKey());
        row.setPrimaryMetricKey(evaluation.primaryMetricKey());
        row.setConfidence(evaluation.confidence());
        row.setRequiredSampleSize(evaluation.requiredSampleSizePerVariant());
        row.setReason(reason(evaluation.reasons()));
        row.setWritebackStatus(evaluation.writebackStatus());
        row.setEvaluatedAt(evaluatedAt);
        decisionMapper.insert(row);
        return evaluation;
    }

    private String reason(List<String> reasons) {
        String value = reasons == null || reasons.isEmpty() ? "experiment governance evaluated" : String.join("; ", reasons);
        return value.length() > 2000 ? value.substring(0, 2000) : value;
    }

    private AbExperimentMetricSnapshotDO bestTreatment(AbExperimentMetricDO metric,
                                                       List<AbExperimentMetricSnapshotDO> snapshots) {
        Comparator<AbExperimentMetricSnapshotDO> comparator = Comparator.comparing(this::value);
        if (DIRECTION_DECREASE.equals(direction(metric))) {
            comparator = comparator.reversed();
        }
        return snapshots.stream().max(comparator).orElseThrow();
    }

    private boolean isPositiveLift(AbExperimentMetricDO metric,
                                   AbExperimentMetricSnapshotDO control,
                                   AbExperimentMetricSnapshotDO treatment) {
        return DIRECTION_DECREASE.equals(direction(metric))
                ? value(treatment).compareTo(value(control)) < 0
                : value(treatment).compareTo(value(control)) > 0;
    }

    private BigDecimal regression(AbExperimentMetricDO metric,
                                  AbExperimentMetricSnapshotDO control,
                                  AbExperimentMetricSnapshotDO treatment) {
        BigDecimal diff = DIRECTION_DECREASE.equals(direction(metric))
                ? value(treatment).subtract(value(control))
                : value(control).subtract(value(treatment));
        return diff.max(BigDecimal.ZERO).setScale(8, RoundingMode.HALF_UP);
    }

    private long requiredSample(AbExperimentMetricDO metric, BigDecimal baseline) {
        long configured = metric.getMinimumSampleSize() == null ? 0L : metric.getMinimumSampleSize();
        BigDecimal effect = positiveOrDefault(metric.getMinimumDetectableEffect(), DEFAULT_MDE);
        double p = clamp(value(baseline), 0.001D, 0.999D);
        double e = Math.max(0.0001D, effect.doubleValue());
        long estimated = (long) Math.ceil((16.0D * p * (1.0D - p)) / (e * e));
        return Math.max(configured, estimated);
    }

    private BigDecimal confidence(AbExperimentMetricDO metric,
                                  AbExperimentMetricSnapshotDO control,
                                  AbExperimentMetricSnapshotDO treatment) {
        double controlValue = value(value(control));
        double treatmentValue = value(value(treatment));
        double effect = DIRECTION_DECREASE.equals(direction(metric))
                ? controlValue - treatmentValue
                : treatmentValue - controlValue;
        double se = Math.sqrt((controlValue * (1.0D - controlValue)) / Math.max(1L, sampleSize(control))
                + (treatmentValue * (1.0D - treatmentValue)) / Math.max(1L, sampleSize(treatment)));
        double confidence = se <= 0.0D
                ? (effect > 0.0D ? 1.0D : 0.5D)
                : normalCdf(effect / se);
        return BigDecimal.valueOf(clamp(confidence, 0.0D, 1.0D))
                .setScale(CONFIDENCE_SCALE, RoundingMode.HALF_UP);
    }

    private double normalCdf(double z) {
        double sign = z < 0 ? -1.0D : 1.0D;
        double abs = Math.abs(z);
        double t = 1.0D / (1.0D + 0.3275911D * abs / Math.sqrt(2.0D));
        double erf = 1.0D - (((((1.061405429D * t - 1.453152027D) * t)
                + 1.421413741D) * t - 0.284496736D) * t + 0.254829592D)
                * t * Math.exp(-abs * abs / 2.0D);
        return 0.5D * (1.0D + sign * erf);
    }

    private AbExperimentMetricSnapshotDO snapshot(
            Map<String, Map<String, AbExperimentMetricSnapshotDO>> snapshots,
            String metricKey,
            String variantKey) {
        return snapshotVariants(snapshots, metricKey).get(variantKey);
    }

    private Map<String, AbExperimentMetricSnapshotDO> snapshotVariants(
            Map<String, Map<String, AbExperimentMetricSnapshotDO>> snapshots,
            String metricKey) {
        return snapshots.getOrDefault(metricKey, Map.of());
    }

    private BigDecimal value(AbExperimentMetricSnapshotDO snapshot) {
        return snapshot == null || snapshot.getMetricValue() == null
                ? BigDecimal.ZERO
                : snapshot.getMetricValue().setScale(8, RoundingMode.HALF_UP);
    }

    private double value(BigDecimal value) {
        return value == null ? 0.0D : value.doubleValue();
    }

    private long sampleSize(AbExperimentMetricSnapshotDO snapshot) {
        return snapshot == null || snapshot.getSampleSize() == null ? 0L : snapshot.getSampleSize();
    }

    private BigDecimal positiveOrDefault(BigDecimal value, BigDecimal fallback) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0 ? fallback : value;
    }

    private String role(AbExperimentMetricDO metric) {
        return normalize(metric.getMetricRole(), ROLE_PRIMARY);
    }

    private String direction(AbExperimentMetricDO metric) {
        String value = normalize(metric.getDirection(), DIRECTION_INCREASE);
        return DIRECTION_DECREASE.equals(value) ? DIRECTION_DECREASE : DIRECTION_INCREASE;
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(Locale.ROOT);
    }

    private String variantKey(String value) {
        if (value == null || value.isBlank()) {
            return "A";
        }
        return value.trim();
    }

    private Long requireId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("experimentId is required");
        }
        return value;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public record Evaluation(
            Long experimentId,
            String status,
            String winnerVariantKey,
            String primaryMetricKey,
            BigDecimal confidence,
            Long requiredSampleSizePerVariant,
            String writebackStatus,
            List<String> reasons,
            List<MetricOutcome> metrics) {
    }

    public record MetricOutcome(
            String metricKey,
            String metricRole,
            String direction,
            String controlVariantKey,
            String treatmentVariantKey,
            BigDecimal controlValue,
            BigDecimal treatmentValue,
            BigDecimal absoluteLift,
            BigDecimal confidence,
            Long controlSampleSize,
            Long treatmentSampleSize,
            Long requiredSampleSizePerVariant) {
    }
}
