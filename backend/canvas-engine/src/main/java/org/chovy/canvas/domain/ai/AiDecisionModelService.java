package org.chovy.canvas.domain.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.AiDecisionFeedbackDO;
import org.chovy.canvas.dal.dataobject.AiDecisionRunDO;
import org.chovy.canvas.dal.dataobject.AiUserDecisionRecommendationDO;
import org.chovy.canvas.dal.dataobject.AiUserPredictionSnapshotDO;
import org.chovy.canvas.dal.dataobject.CdpUserProfileDO;
import org.chovy.canvas.dal.dataobject.MarketingConsentDO;
import org.chovy.canvas.dal.mapper.AiDecisionFeedbackMapper;
import org.chovy.canvas.dal.mapper.AiDecisionRunMapper;
import org.chovy.canvas.dal.mapper.AiUserDecisionRecommendationMapper;
import org.chovy.canvas.dal.mapper.AiUserPredictionSnapshotMapper;
import org.chovy.canvas.dal.mapper.CdpUserProfileMapper;
import org.chovy.canvas.dal.mapper.MarketingConsentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class AiDecisionModelService {

    public static final String MODEL_KEY = "AI_DECISION_BASELINE";

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ChurnFeatureSnapshotService featureSnapshotService;
    private final SmartTimingService smartTimingService;
    private final AiDecisionRunMapper runMapper;
    private final AiUserDecisionRecommendationMapper recommendationMapper;
    private final AiDecisionFeedbackMapper feedbackMapper;
    private final CdpUserProfileMapper profileMapper;
    private final MarketingConsentMapper consentMapper;
    private final AiUserPredictionSnapshotMapper snapshotMapper;
    private final AiPredictionProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public AiDecisionModelService(ChurnFeatureSnapshotService featureSnapshotService,
                                  SmartTimingService smartTimingService,
                                  AiDecisionRunMapper runMapper,
                                  AiUserDecisionRecommendationMapper recommendationMapper,
                                  AiDecisionFeedbackMapper feedbackMapper,
                                  CdpUserProfileMapper profileMapper,
                                  MarketingConsentMapper consentMapper,
                                  AiUserPredictionSnapshotMapper snapshotMapper,
                                  AiPredictionProperties properties,
                                  ObjectMapper objectMapper) {
        this(featureSnapshotService, smartTimingService, runMapper, recommendationMapper, feedbackMapper,
                profileMapper, consentMapper, snapshotMapper, properties, objectMapper, Clock.systemDefaultZone());
    }

    AiDecisionModelService(ChurnFeatureSnapshotService featureSnapshotService,
                           SmartTimingService smartTimingService,
                           AiDecisionRunMapper runMapper,
                           AiUserDecisionRecommendationMapper recommendationMapper,
                           AiDecisionFeedbackMapper feedbackMapper,
                           CdpUserProfileMapper profileMapper,
                           MarketingConsentMapper consentMapper,
                           AiUserPredictionSnapshotMapper snapshotMapper,
                           AiPredictionProperties properties,
                           ObjectMapper objectMapper,
                           Clock clock) {
        this.featureSnapshotService = featureSnapshotService;
        this.smartTimingService = smartTimingService;
        this.runMapper = runMapper;
        this.recommendationMapper = recommendationMapper;
        this.feedbackMapper = feedbackMapper;
        this.profileMapper = profileMapper;
        this.consentMapper = consentMapper;
        this.snapshotMapper = snapshotMapper;
        this.properties = properties == null ? new AiPredictionProperties() : properties;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public AiDecisionRunView recompute(Long tenantId, AiDecisionRecomputeCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("AI decision recompute command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDate runDate = command.runDate() == null ? LocalDate.now(clock) : command.runDate();
        String decisionScope = normalizeUpper(command.decisionScope(), "DAILY_MARKETING");
        LocalDateTime startedAt = now();
        List<String> userIds = boundedUsers(command.userIds());
        AiDecisionRunDO run = insertRun(scopedTenantId, runDate, decisionScope, userIds.size(), command.metadata(),
                actor, startedAt);
        int processed = 0;
        int skipped = 0;
        int failed = 0;
        try {
            for (String userId : userIds) {
                CdpUserProfileDO profile = profileMapper.selectOne(new LambdaQueryWrapper<CdpUserProfileDO>()
                        .eq(CdpUserProfileDO::getTenantId, scopedTenantId)
                        .eq(CdpUserProfileDO::getUserId, userId)
                        .last("LIMIT 1"));
                if (profile == null || !scopedTenantId.equals(profile.getTenantId())) {
                    skipped++;
                    continue;
                }
                DecisionFeatures features = features(userId, runDate);
                List<String> channels = consentChannels(scopedTenantId, userId);
                writeRecommendations(scopedTenantId, run, profile, features, channels, command.budgetCap(), startedAt);
                processed++;
            }
            finishRun(run, "SUCCESS", processed, skipped, failed, null);
            return toRunView(run);
        } catch (RuntimeException ex) {
            failed++;
            finishRun(run, "FAILED", processed, skipped, failed, ex.getMessage());
            throw ex;
        }
    }

    public List<AiDecisionRecommendationView> recommendations(Long tenantId, AiDecisionRecommendationQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("AI decision recommendation query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        int limit = boundedLimit(query.limit());
        String decisionType = optionalUpper(query.decisionType());
        String eligibilityStatus = optionalUpper(query.eligibilityStatus());
        return safeList(recommendationMapper.selectList(new LambdaQueryWrapper<AiUserDecisionRecommendationDO>()
                        .eq(AiUserDecisionRecommendationDO::getTenantId, scopedTenantId)
                        .eq(query.runId() != null, AiUserDecisionRecommendationDO::getRunId, query.runId())
                        .eq(decisionType != null, AiUserDecisionRecommendationDO::getDecisionType, decisionType)
                        .eq(eligibilityStatus != null, AiUserDecisionRecommendationDO::getEligibilityStatus, eligibilityStatus)
                        .orderByAsc(AiUserDecisionRecommendationDO::getRecommendationRank)
                        .last("LIMIT " + limit)))
                .stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> query.runId() == null || query.runId().equals(row.getRunId()))
                .filter(row -> decisionType == null || decisionType.equals(row.getDecisionType()))
                .filter(row -> eligibilityStatus == null || eligibilityStatus.equals(row.getEligibilityStatus()))
                .limit(limit)
                .map(this::toRecommendationView)
                .toList();
    }

    public Optional<AiDecisionRunView> latestRun(Long tenantId, String decisionScope) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedDecisionScope = normalizeUpper(decisionScope, "DAILY_MARKETING");
        return safeList(runMapper.selectList(new LambdaQueryWrapper<AiDecisionRunDO>()
                        .eq(AiDecisionRunDO::getTenantId, scopedTenantId)
                        .eq(AiDecisionRunDO::getModelKey, MODEL_KEY)
                        .eq(AiDecisionRunDO::getDecisionScope, scopedDecisionScope)
                        .orderByDesc(AiDecisionRunDO::getStartedAt)
                        .orderByDesc(AiDecisionRunDO::getId)
                        .last("LIMIT 1")))
                .stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> MODEL_KEY.equals(row.getModelKey()))
                .filter(row -> scopedDecisionScope.equals(row.getDecisionScope()))
                .findFirst()
                .map(this::toRunView);
    }

    public AiDecisionFeedbackView recordFeedback(Long tenantId,
                                                 Long recommendationId,
                                                 AiDecisionFeedbackCommand command,
                                                 String actor) {
        if (command == null) {
            throw new IllegalArgumentException("AI decision feedback command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        AiUserDecisionRecommendationDO recommendation = recommendationMapper.selectById(recommendationId);
        if (recommendation == null || !scopedTenantId.equals(recommendation.getTenantId())) {
            throw new IllegalArgumentException("recommendation is not found");
        }
        LocalDateTime occurredAt = now();
        AiDecisionFeedbackDO row = new AiDecisionFeedbackDO();
        row.setTenantId(scopedTenantId);
        row.setRecommendationId(recommendationId);
        row.setFeedbackType(normalizeUpper(command.feedbackType(), "UNKNOWN"));
        row.setOutcomeValue(command.outcomeValue());
        row.setMetadataJson(json(command.metadata()));
        row.setCreatedBy(defaultString(actor, "system"));
        row.setOccurredAt(occurredAt);
        row.setCreatedAt(occurredAt);
        feedbackMapper.insert(row);
        return toFeedbackView(row);
    }

    private AiDecisionRunDO insertRun(Long tenantId,
                                      LocalDate runDate,
                                      String decisionScope,
                                      int requestedCount,
                                      Map<String, Object> metadata,
                                      String actor,
                                      LocalDateTime startedAt) {
        AiDecisionRunDO row = new AiDecisionRunDO();
        row.setTenantId(tenantId);
        row.setModelKey(MODEL_KEY);
        row.setModelVersion(properties.getModelVersion());
        row.setDecisionScope(decisionScope);
        row.setRunDate(runDate);
        row.setStatus("RUNNING");
        row.setRequestedCount(requestedCount);
        row.setProcessedCount(0);
        row.setSkippedCount(0);
        row.setFailedCount(0);
        row.setMetadataJson(json(metadata));
        row.setCreatedBy(defaultString(actor, "system"));
        row.setStartedAt(startedAt);
        row.setCreatedAt(startedAt);
        row.setUpdatedAt(startedAt);
        runMapper.insert(row);
        return row;
    }

    private void finishRun(AiDecisionRunDO run,
                           String status,
                           int processed,
                           int skipped,
                           int failed,
                           String errorMessage) {
        LocalDateTime finishedAt = now();
        run.setStatus(status);
        run.setProcessedCount(processed);
        run.setSkippedCount(skipped);
        run.setFailedCount(failed);
        run.setErrorMessage(errorMessage);
        run.setFinishedAt(finishedAt);
        run.setUpdatedAt(finishedAt);
        runMapper.updateById(run);
    }

    private DecisionFeatures features(String userId, LocalDate runDate) {
        ChurnFeatureSnapshotService.FeatureSnapshot featureSnapshot = featureSnapshotService.extract(userId, runDate);
        int bestSendHour = smartTimingService.bestSendHour(userId, runDate);
        AiUserPredictionSnapshotDO churn = safeList(snapshotMapper.selectList(
                        new LambdaQueryWrapper<AiUserPredictionSnapshotDO>()
                                .eq(AiUserPredictionSnapshotDO::getUserId, userId)
                                .eq(AiUserPredictionSnapshotDO::getModelKey, ChurnPredictionService.MODEL_KEY)
                                .orderByDesc(AiUserPredictionSnapshotDO::getRunId)
                                .last("LIMIT 1")))
                .stream()
                .findFirst()
                .orElse(null);
        BigDecimal churnProbability = churn == null || churn.getChurnProbability() == null
                ? BigDecimal.ZERO
                : scale(churn.getChurnProbability());
        String churnBand = churn == null ? "UNKNOWN" : defaultString(churn.getChurnRiskBand(), "UNKNOWN");
        return new DecisionFeatures(featureSnapshot, bestSendHour, churnProbability, churnBand);
    }

    private void writeRecommendations(Long tenantId,
                                      AiDecisionRunDO run,
                                      CdpUserProfileDO profile,
                                      DecisionFeatures features,
                                      List<String> consentChannels,
                                      BigDecimal budgetCap,
                                      LocalDateTime createdAt) {
        Map<String, Object> featureMap = featureMap(profile, features, consentChannels);
        String fallbackReason = features.snapshot().sparseHistory() ? "SPARSE_HISTORY" : null;
        BigDecimal confidence = fallbackReason == null ? new BigDecimal("0.70000") : new BigDecimal("0.40000");
        BigDecimal ltvScore = score(0.35d
                + Math.min(features.snapshot().goalCount30d(), 5) * 0.08d
                + Math.min(features.snapshot().eventCount30d(), 20) * 0.01d
                + Math.min(features.snapshot().profileAgeDays(), 365) / 3650.0d
                - features.churnProbability().doubleValue() * 0.15d);
        BigDecimal actionScore = score(0.30d + features.churnProbability().doubleValue() * 0.65d
                + (features.snapshot().sparseHistory() ? -0.10d : 0.0d));
        BigDecimal offerScore = score(0.25d + features.churnProbability().doubleValue() * 0.55d
                + Math.min(features.snapshot().goalCount30d(), 4) * 0.04d);
        BigDecimal channelScore = score(0.30d + consentChannels.size() * 0.18d
                - features.snapshot().deliveryFailureRate30d() * 0.30d);
        insertRecommendation(tenantId, run, profile.getUserId(), "LTV", "VALUE_TIER",
                "VALUE_TIER", null, null, ltvScore, confidence, 1, BigDecimal.ZERO, "ELIGIBLE",
                fallbackReason, featureMap, explanation("LTV", features), createdAt);
        insertRecommendation(tenantId, run, profile.getUserId(), "NEXT_BEST_ACTION", actionKey(features),
                actionKey(features), null, null, actionScore, confidence, 2, new BigDecimal("0.0000"), "ELIGIBLE",
                fallbackReason, featureMap, explanation("NEXT_BEST_ACTION", features), createdAt);
        BigDecimal offerCost = offerCost(features);
        boolean budgetConstrained = budgetCap != null && offerCost.compareTo(budgetCap) > 0;
        insertRecommendation(tenantId, run, profile.getUserId(), "NEXT_BEST_OFFER", offerKey(features),
                null, offerKey(features), null, offerScore, confidence, 3, offerCost,
                budgetConstrained ? "BUDGET_CONSTRAINED" : "ELIGIBLE",
                budgetConstrained ? "BUDGET_CAP_EXCEEDED" : fallbackReason,
                featureMap, explanation("NEXT_BEST_OFFER", features), createdAt);
        String channel = preferredChannel(consentChannels, profile);
        insertRecommendation(tenantId, run, profile.getUserId(), "CHANNEL_AFFINITY", channel,
                null, null, channel, channelScore, confidence, 4, BigDecimal.ZERO, "ELIGIBLE",
                fallbackReason, featureMap, explanation("CHANNEL_AFFINITY", features), createdAt);
    }

    private void insertRecommendation(Long tenantId,
                                      AiDecisionRunDO run,
                                      String userId,
                                      String decisionType,
                                      String decisionKey,
                                      String actionKey,
                                      String offerKey,
                                      String channel,
                                      BigDecimal score,
                                      BigDecimal confidence,
                                      int rank,
                                      BigDecimal budgetCost,
                                      String eligibilityStatus,
                                      String fallbackReason,
                                      Map<String, Object> features,
                                      Map<String, Object> explanation,
                                      LocalDateTime createdAt) {
        AiUserDecisionRecommendationDO row = new AiUserDecisionRecommendationDO();
        row.setTenantId(tenantId);
        row.setRunId(run.getId());
        row.setUserId(userId);
        row.setModelKey(MODEL_KEY);
        row.setModelVersion(properties.getModelVersion());
        row.setDecisionScope(run.getDecisionScope());
        row.setDecisionType(decisionType);
        row.setDecisionKey(decisionKey);
        row.setActionKey(actionKey);
        row.setOfferKey(offerKey);
        row.setChannel(channel);
        row.setScore(score);
        row.setConfidence(confidence);
        row.setRecommendationRank(rank);
        row.setBudgetCost(budgetCost);
        row.setEligibilityStatus(eligibilityStatus);
        row.setFallbackReason(fallbackReason);
        row.setFeatureJson(json(features));
        row.setExplanationJson(json(explanation));
        row.setCreatedAt(createdAt);
        row.setUpdatedAt(createdAt);
        recommendationMapper.insert(row);
    }

    private Map<String, Object> featureMap(CdpUserProfileDO profile,
                                           DecisionFeatures features,
                                           List<String> consentChannels) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", profile.getUserId());
        map.put("emailReachable", hasText(profile.getEmail()));
        map.put("phoneReachable", hasText(profile.getPhone()));
        map.put("idleDays", features.snapshot().daysSinceLastEvent());
        map.put("eventCount30d", features.snapshot().eventCount30d());
        map.put("sendCount30d", features.snapshot().sendCount30d());
        map.put("deliveryFailureRate30d", features.snapshot().deliveryFailureRate30d());
        map.put("goalCount30d", features.snapshot().goalCount30d());
        map.put("profileAgeDays", features.snapshot().profileAgeDays());
        map.put("sparseHistory", features.snapshot().sparseHistory());
        map.put("churnProbability", features.churnProbability());
        map.put("churnRiskBand", features.churnBand());
        map.put("bestSendHour", features.bestSendHour());
        map.put("consentChannels", consentChannels);
        return map;
    }

    private Map<String, Object> explanation(String decisionType, DecisionFeatures features) {
        return Map.of(
                "decisionType", decisionType,
                "contributions", List.of(
                        Map.of("feature", "churnProbability", "value", features.churnProbability()),
                        Map.of("feature", "goalCount30d", "value", features.snapshot().goalCount30d()),
                        Map.of("feature", "bestSendHour", "value", features.bestSendHour())));
    }

    private List<String> consentChannels(Long tenantId, String userId) {
        return safeList(consentMapper.selectList(new LambdaQueryWrapper<MarketingConsentDO>()
                        .eq(MarketingConsentDO::getTenantId, tenantId)
                        .eq(MarketingConsentDO::getUserId, userId)
                        .eq(MarketingConsentDO::getConsentStatus, MarketingConsentDO.OPT_IN)))
                .stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .filter(row -> MarketingConsentDO.OPT_IN.equals(row.getConsentStatus()))
                .map(MarketingConsentDO::getChannel)
                .filter(this::hasText)
                .map(channel -> channel.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private String actionKey(DecisionFeatures features) {
        if (features.churnProbability().compareTo(new BigDecimal("0.70000")) >= 0) {
            return "RETENTION_INTERVENTION";
        }
        if (features.snapshot().goalCount30d() > 0) {
            return "LOYALTY_NURTURE";
        }
        return "CROSS_SELL";
    }

    private String offerKey(DecisionFeatures features) {
        if (features.churnProbability().compareTo(new BigDecimal("0.70000")) >= 0) {
            return "SERVICE_BENEFIT";
        }
        return features.snapshot().goalCount30d() > 0 ? "POINTS_BONUS" : "NO_OFFER";
    }

    private BigDecimal offerCost(DecisionFeatures features) {
        if (features.churnProbability().compareTo(new BigDecimal("0.70000")) >= 0) {
            return new BigDecimal("25.0000");
        }
        return new BigDecimal("5.0000");
    }

    private String preferredChannel(List<String> consentChannels, CdpUserProfileDO profile) {
        if (consentChannels.contains("EMAIL") && hasText(profile.getEmail())) {
            return "EMAIL";
        }
        if (consentChannels.contains("SMS") && hasText(profile.getPhone())) {
            return "SMS";
        }
        return "IN_APP";
    }

    private AiDecisionRunView toRunView(AiDecisionRunDO row) {
        return new AiDecisionRunView(
                row.getId(),
                row.getTenantId(),
                row.getModelKey(),
                row.getModelVersion(),
                row.getDecisionScope(),
                row.getRunDate(),
                row.getStatus(),
                defaultInt(row.getRequestedCount()),
                defaultInt(row.getProcessedCount()),
                defaultInt(row.getSkippedCount()),
                defaultInt(row.getFailedCount()),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getStartedAt(),
                row.getFinishedAt(),
                row.getErrorMessage());
    }

    private AiDecisionRecommendationView toRecommendationView(AiUserDecisionRecommendationDO row) {
        return new AiDecisionRecommendationView(
                row.getId(),
                row.getTenantId(),
                row.getRunId(),
                row.getUserId(),
                row.getModelKey(),
                row.getModelVersion(),
                row.getDecisionScope(),
                row.getDecisionType(),
                row.getDecisionKey(),
                row.getActionKey(),
                row.getOfferKey(),
                row.getChannel(),
                row.getScore(),
                row.getConfidence(),
                row.getRecommendationRank(),
                row.getBudgetCost(),
                row.getEligibilityStatus(),
                row.getFallbackReason(),
                map(row.getFeatureJson()),
                map(row.getExplanationJson()),
                row.getCreatedAt());
    }

    private AiDecisionFeedbackView toFeedbackView(AiDecisionFeedbackDO row) {
        return new AiDecisionFeedbackView(
                row.getId(),
                row.getTenantId(),
                row.getRecommendationId(),
                row.getFeedbackType(),
                row.getOutcomeValue(),
                map(row.getMetadataJson()),
                row.getCreatedBy(),
                row.getOccurredAt());
    }

    private List<String> boundedUsers(List<String> requestedUsers) {
        Set<String> users = new LinkedHashSet<>();
        for (String userId : requestedUsers == null ? List.<String>of() : requestedUsers) {
            if (hasText(userId)) {
                users.add(userId.trim());
            }
        }
        int limit = Math.max(1, properties.getBatchSize());
        return new ArrayList<>(users).stream().limit(limit).toList();
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("AI decision JSON serialization failed", ex);
        }
    }

    private Map<String, Object> map(String json) {
        if (!hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private BigDecimal score(double value) {
        double bounded = Math.max(0.0d, Math.min(1.0d, value));
        return BigDecimal.valueOf(bounded).setScale(5, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(5, RoundingMode.HALF_UP);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String normalizeUpper(String value, String fallback) {
        return defaultString(value, fallback).toUpperCase(Locale.ROOT);
    }

    private String optionalUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private record DecisionFeatures(
            ChurnFeatureSnapshotService.FeatureSnapshot snapshot,
            int bestSendHour,
            BigDecimal churnProbability,
            String churnBand) {
    }
}
