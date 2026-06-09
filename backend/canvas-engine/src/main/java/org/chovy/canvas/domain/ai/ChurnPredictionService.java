package org.chovy.canvas.domain.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.AiPredictionRunDO;
import org.chovy.canvas.dal.dataobject.AiUserPredictionSnapshotDO;
import org.chovy.canvas.dal.mapper.AiPredictionRunMapper;
import org.chovy.canvas.dal.mapper.AiUserPredictionSnapshotMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChurnPredictionService {

    public static final String MODEL_KEY = "churn_prediction";
    public static final String BAND_HIGH = "HIGH";
    public static final String BAND_MEDIUM = "MEDIUM";
    public static final String BAND_LOW = "LOW";

    private final ChurnFeatureSnapshotService featureSnapshotService;
    private final SmartTimingService smartTimingService;
    private final PredictionProfileWriter profileWriter;
    private final AiPredictionRunMapper runMapper;
    private final AiUserPredictionSnapshotMapper snapshotMapper;
    private final AiPredictionProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public ChurnPredictionService(ChurnFeatureSnapshotService featureSnapshotService,
                                  SmartTimingService smartTimingService,
                                  PredictionProfileWriter profileWriter,
                                  AiPredictionRunMapper runMapper,
                                  AiUserPredictionSnapshotMapper snapshotMapper,
                                  AiPredictionProperties properties,
                                  ObjectMapper objectMapper) {
        this(featureSnapshotService, smartTimingService, profileWriter, runMapper, snapshotMapper,
                properties, objectMapper, Clock.systemDefaultZone());
    }

    ChurnPredictionService(ChurnFeatureSnapshotService featureSnapshotService,
                           SmartTimingService smartTimingService,
                           PredictionProfileWriter profileWriter,
                           AiPredictionRunMapper runMapper,
                           AiUserPredictionSnapshotMapper snapshotMapper,
                           AiPredictionProperties properties,
                           ObjectMapper objectMapper,
                           Clock clock) {
        this.featureSnapshotService = featureSnapshotService;
        this.smartTimingService = smartTimingService;
        this.profileWriter = profileWriter;
        this.runMapper = runMapper;
        this.snapshotMapper = snapshotMapper;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public PredictionRunView recompute(Long tenantId, RecomputeRequest request) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("canvas.ai.prediction.enabled must be true to recompute predictions");
        }
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        LocalDate runDate = request == null || request.runDate() == null ? LocalDate.now(clock) : request.runDate();
        String modelVersion = modelVersion();
        AiPredictionRunDO run = findRun(scopedTenantId, runDate, modelVersion).orElse(null);
        if (run != null && AiPredictionRunDO.STATUS_SUCCESS.equals(run.getStatus())
                && (request == null || !request.force())) {
            return toRunView(run);
        }
        if (run == null) {
            run = newRun(scopedTenantId, runDate, modelVersion);
            runMapper.insert(run);
        } else {
            snapshotMapper.delete(new LambdaQueryWrapper<AiUserPredictionSnapshotDO>()
                    .eq(AiUserPredictionSnapshotDO::getTenantId, scopedTenantId)
                    .eq(AiUserPredictionSnapshotDO::getRunId, run.getId()));
            run.setStatus(AiPredictionRunDO.STATUS_RUNNING);
            run.setProcessedCount(0);
            run.setSkippedCount(0);
            run.setFailedCount(0);
            run.setStartedAt(LocalDateTime.now(clock));
            run.setFinishedAt(null);
            run.setErrorMessage(null);
            runMapper.updateById(run);
        }

        int processed = 0;
        int skipped = 0;
        int failed = 0;
        int limit = request == null || request.limit() == null ? properties.getBatchSize() : request.limit();
        List<String> userIds = featureSnapshotService.candidateUserIds(limit);
        for (String userId : userIds) {
            try {
                Prediction prediction = predict(featureSnapshotService.extract(userId, runDate));
                int bestSendHour = smartTimingService.bestSendHour(userId, runDate);
                AiUserPredictionSnapshotDO snapshot = toSnapshot(scopedTenantId, run, prediction, bestSendHour);
                snapshotMapper.insert(snapshot);
                profileWriter.write(userId, prediction.probability(), prediction.band(), bestSendHour,
                        LocalDateTime.now(clock));
                processed++;
            } catch (Exception ignored) {
                failed++;
            }
        }
        skipped = Math.max(0, userIds.size() - processed - failed);
        run.setStatus(AiPredictionRunDO.STATUS_SUCCESS);
        run.setProcessedCount(processed);
        run.setSkippedCount(skipped);
        run.setFailedCount(failed);
        run.setFinishedAt(LocalDateTime.now(clock));
        runMapper.updateById(run);
        return toRunView(run);
    }

    public Prediction predict(ChurnFeatureSnapshotService.FeatureSnapshot snapshot) {
        double idleContribution = Math.min(snapshot.daysSinceLastEvent(), 60) * 0.012d;
        double failureContribution = snapshot.deliveryFailureRate30d() * 0.20d;
        double engagementContribution = -Math.min(snapshot.eventCount30d(), 30) * 0.006d;
        double goalContribution = -Math.min(snapshot.goalCount30d(), 10) * 0.025d;
        double raw = 0.20d + idleContribution + failureContribution + engagementContribution + goalContribution;
        double probability = snapshot.sparseHistory() ? 0.50d : Math.max(0.05d, Math.min(0.95d, raw));
        String band = riskBand(probability);
        BigDecimal confidence = scaled(snapshot.sparseHistory() ? 0.30d : 0.80d);
        Map<String, Object> features = new LinkedHashMap<>();
        features.put("userId", snapshot.userId());
        features.put("daysSinceLastEvent", snapshot.daysSinceLastEvent());
        features.put("eventCount30d", snapshot.eventCount30d());
        features.put("sendCount30d", snapshot.sendCount30d());
        features.put("deliveryFailureRate30d", snapshot.deliveryFailureRate30d());
        features.put("goalCount30d", snapshot.goalCount30d());
        features.put("profileAgeDays", snapshot.profileAgeDays());
        features.put("sparseHistory", snapshot.sparseHistory());
        Map<String, Object> contributions = new LinkedHashMap<>();
        contributions.put("base", 0.20d);
        contributions.put("idleDays", idleContribution);
        contributions.put("failures", failureContribution);
        contributions.put("engagement", engagementContribution);
        contributions.put("goals", goalContribution);
        return new Prediction(
                snapshot.userId(),
                scaled(probability),
                band,
                confidence,
                features,
                contributions);
    }

    public Optional<PredictionRunView> latestRun(Long tenantId) {
        return latestRunDO(tenantId == null ? 0L : tenantId).map(this::toRunView);
    }

    public PredictionReadinessView readiness(Long tenantId) {
        boolean enabled = properties.isEnabled();
        return new PredictionReadinessView(
                enabled,
                enabled ? null : "canvas.ai.prediction.enabled must be true to recompute predictions",
                modelVersion(),
                properties.getBatchSize());
    }

    public List<RiskDistributionItem> churnDistribution(Long tenantId) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        AiPredictionRunDO run = latestRunDO(scopedTenantId).orElse(null);
        if (run == null) {
            return List.of();
        }
        Map<String, Long> counts = snapshotsForRun(scopedTenantId, run.getId()).stream()
                .collect(Collectors.groupingBy(AiUserPredictionSnapshotDO::getChurnRiskBand, Collectors.counting()));
        return List.of(BAND_HIGH, BAND_MEDIUM, BAND_LOW).stream()
                .map(band -> new RiskDistributionItem(band, counts.getOrDefault(band, 0L)))
                .toList();
    }

    public List<TopRiskUser> topRiskUsers(Long tenantId, int limit) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        AiPredictionRunDO run = latestRunDO(scopedTenantId).orElse(null);
        if (run == null) {
            return List.of();
        }
        int boundedLimit = Math.max(1, Math.min(limit <= 0 ? 100 : limit, 500));
        return snapshotsForRun(scopedTenantId, run.getId()).stream()
                .sorted(Comparator
                        .comparing(AiUserPredictionSnapshotDO::getChurnProbability, Comparator.nullsLast(BigDecimal::compareTo))
                        .reversed()
                        .thenComparing(AiUserPredictionSnapshotDO::getUserId, Comparator.nullsLast(String::compareTo)))
                .limit(boundedLimit)
                .map(snapshot -> new TopRiskUser(
                        snapshot.getUserId(),
                        snapshot.getChurnProbability(),
                        snapshot.getChurnRiskBand(),
                        snapshot.getBestSendHour(),
                        snapshot.getConfidence()))
                .toList();
    }

    private Optional<AiPredictionRunDO> latestRunDO(Long tenantId) {
        return runMapper.selectList(new LambdaQueryWrapper<AiPredictionRunDO>()
                        .eq(AiPredictionRunDO::getTenantId, tenantId)
                        .eq(AiPredictionRunDO::getModelKey, MODEL_KEY)
                        .eq(AiPredictionRunDO::getModelVersion, modelVersion())
                        .orderByDesc(AiPredictionRunDO::getStartedAt)
                        .last("LIMIT 1"))
                .stream()
                .findFirst();
    }

    private Optional<AiPredictionRunDO> findRun(Long tenantId, LocalDate runDate, String modelVersion) {
        return Optional.ofNullable(runMapper.selectOne(new LambdaQueryWrapper<AiPredictionRunDO>()
                .eq(AiPredictionRunDO::getTenantId, tenantId)
                .eq(AiPredictionRunDO::getModelKey, MODEL_KEY)
                .eq(AiPredictionRunDO::getModelVersion, modelVersion)
                .eq(AiPredictionRunDO::getRunDate, runDate)
                .last("LIMIT 1")));
    }

    private List<AiUserPredictionSnapshotDO> snapshotsForRun(Long tenantId, Long runId) {
        return snapshotMapper.selectList(new LambdaQueryWrapper<AiUserPredictionSnapshotDO>()
                .eq(AiUserPredictionSnapshotDO::getTenantId, tenantId)
                .eq(AiUserPredictionSnapshotDO::getRunId, runId));
    }

    private AiPredictionRunDO newRun(Long tenantId, LocalDate runDate, String modelVersion) {
        AiPredictionRunDO run = new AiPredictionRunDO();
        run.setTenantId(tenantId);
        run.setModelKey(MODEL_KEY);
        run.setModelVersion(modelVersion);
        run.setRunDate(runDate);
        run.setStatus(AiPredictionRunDO.STATUS_RUNNING);
        run.setProcessedCount(0);
        run.setSkippedCount(0);
        run.setFailedCount(0);
        run.setStartedAt(LocalDateTime.now(clock));
        return run;
    }

    private AiUserPredictionSnapshotDO toSnapshot(Long tenantId,
                                                  AiPredictionRunDO run,
                                                  Prediction prediction,
                                                  int bestSendHour) {
        AiUserPredictionSnapshotDO snapshot = new AiUserPredictionSnapshotDO();
        snapshot.setTenantId(tenantId);
        snapshot.setRunId(run.getId());
        snapshot.setUserId(prediction.userId());
        snapshot.setModelKey(MODEL_KEY);
        snapshot.setModelVersion(run.getModelVersion());
        snapshot.setChurnProbability(prediction.probability());
        snapshot.setChurnRiskBand(prediction.band());
        snapshot.setBestSendHour(bestSendHour);
        snapshot.setConfidence(prediction.confidence());
        snapshot.setFeatureJson(toJson(prediction.features()));
        snapshot.setContributionJson(toJson(prediction.contributions()));
        return snapshot;
    }

    private PredictionRunView toRunView(AiPredictionRunDO run) {
        return new PredictionRunView(
                run.getId(),
                run.getTenantId(),
                run.getModelKey(),
                run.getModelVersion(),
                run.getRunDate(),
                run.getStatus(),
                nullToZero(run.getProcessedCount()),
                nullToZero(run.getSkippedCount()),
                nullToZero(run.getFailedCount()),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getErrorMessage());
    }

    private String modelVersion() {
        return properties.getModelVersion() == null || properties.getModelVersion().isBlank()
                ? "baseline_v1"
                : properties.getModelVersion().trim();
    }

    private String riskBand(double probability) {
        if (probability >= 0.70d) {
            return BAND_HIGH;
        }
        if (probability >= 0.40d) {
            return BAND_MEDIUM;
        }
        return BAND_LOW;
    }

    private BigDecimal scaled(double value) {
        return BigDecimal.valueOf(value).setScale(5, RoundingMode.HALF_UP);
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("prediction payload cannot be serialized", e);
        }
    }

    public record RecomputeRequest(boolean force, LocalDate runDate, Integer limit) {
    }

    public record PredictionReadinessView(
            boolean recomputeEnabled,
            String disabledReason,
            String modelVersion,
            int batchSize) {
    }

    public record Prediction(
            String userId,
            BigDecimal probability,
            String band,
            BigDecimal confidence,
            Map<String, Object> features,
            Map<String, Object> contributions) {
    }

    public record PredictionRunView(
            Long id,
            Long tenantId,
            String modelKey,
            String modelVersion,
            LocalDate runDate,
            String status,
            int processedCount,
            int skippedCount,
            int failedCount,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            String errorMessage) {
    }

    public record RiskDistributionItem(String band, long count) {
    }

    public record TopRiskUser(
            String userId,
            BigDecimal churnProbability,
            String churnRiskBand,
            Integer bestSendHour,
            BigDecimal confidence) {
    }
}
