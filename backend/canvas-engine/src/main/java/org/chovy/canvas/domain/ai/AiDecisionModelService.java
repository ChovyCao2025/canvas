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

/**
 * AiDecisionModelService 编排 domain.ai 场景的领域业务规则。
 */
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

    /**
     * 创建 AiDecisionModelService 实例并注入 domain.ai 场景依赖。
     * @param featureSnapshotService 依赖组件，用于完成数据访问或外部能力调用。
     * @param smartTimingService 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param recommendationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param feedbackMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param consentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param snapshotMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param properties 配置对象，用于控制运行参数和策略开关。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 执行 AiDecisionModelService 流程，围绕 ai decision model service 完成校验、计算或结果组装。
     *
     * @param featureSnapshotService 依赖组件，用于完成数据访问或外部能力调用。
     * @param smartTimingService 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param recommendationMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param feedbackMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param profileMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param consentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param snapshotMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param properties 配置对象，用于控制运行参数和策略开关。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
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

    /**
     * 在指定租户下重新计算 AI 营销决策，按命令中的用户、日期和决策范围生成一次运行记录。
     * 方法会校验用户归属租户，读取画像、近期特征和营销同意渠道，写入推荐结果，并在运行成功或失败时更新运行状态；
     * {@code actor} 会落到运行记录的创建人字段，作为后续审计追踪来源。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException ex) {
            failed++;
            finishRun(run, "FAILED", processed, skipped, failed, ex.getMessage());
            throw ex;
        }
    }

    /**
     * 查询租户内 AI 决策推荐列表，可按运行批次、决策类型和资格状态过滤。
     * 返回值只包含当前租户可见的数据，并按推荐排序截断到请求上限，避免跨租户暴露推荐详情。
     */
    public List<AiDecisionRecommendationView> recommendations(Long tenantId, AiDecisionRecommendationQuery query) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (query == null) {
            throw new IllegalArgumentException("AI decision recommendation query is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        int limit = boundedLimit(query.limit());
        String decisionType = optionalUpper(query.decisionType());
        String eligibilityStatus = optionalUpper(query.eligibilityStatus());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(recommendationMapper.selectList(new LambdaQueryWrapper<AiUserDecisionRecommendationDO>()
                        .eq(AiUserDecisionRecommendationDO::getTenantId, scopedTenantId)
                        .eq(query.runId() != null, AiUserDecisionRecommendationDO::getRunId, query.runId())
                        .eq(decisionType != null, AiUserDecisionRecommendationDO::getDecisionType, decisionType)
                        .eq(eligibilityStatus != null, AiUserDecisionRecommendationDO::getEligibilityStatus, eligibilityStatus)
                        .orderByAsc(AiUserDecisionRecommendationDO::getRecommendationRank)
                        .last("LIMIT " + limit)))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> query.runId() == null || query.runId().equals(row.getRunId()))
                .filter(row -> decisionType == null || decisionType.equals(row.getDecisionType()))
                .filter(row -> eligibilityStatus == null || eligibilityStatus.equals(row.getEligibilityStatus()))
                .limit(limit)
                .map(this::toRecommendationView)
                .toList();
    }

    /**
     * 获取租户在指定决策范围内最近一次基线 AI 决策运行。
     * 未传范围时使用日常营销范围，返回 {@link Optional#empty()} 表示该租户尚无可用运行记录。
     */
    public Optional<AiDecisionRunView> latestRun(Long tenantId, String decisionScope) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String scopedDecisionScope = normalizeUpper(decisionScope, "DAILY_MARKETING");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(runMapper.selectList(new LambdaQueryWrapper<AiDecisionRunDO>()
                        .eq(AiDecisionRunDO::getTenantId, scopedTenantId)
                        .eq(AiDecisionRunDO::getModelKey, MODEL_KEY)
                        .eq(AiDecisionRunDO::getDecisionScope, scopedDecisionScope)
                        .orderByDesc(AiDecisionRunDO::getStartedAt)
                        .orderByDesc(AiDecisionRunDO::getId)
                        .last("LIMIT 1")))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .filter(row -> scopedTenantId.equals(row.getTenantId()))
                .filter(row -> MODEL_KEY.equals(row.getModelKey()))
                .filter(row -> scopedDecisionScope.equals(row.getDecisionScope()))
                .findFirst()
                .map(this::toRunView);
    }

    /**
     * 为指定推荐记录写入业务反馈，要求推荐必须归属于当前租户。
     * 反馈类型、结果值和元数据会持久化为后续模型评估信号，{@code actor} 作为创建人记录审计来源。
     */
    public AiDecisionFeedbackView recordFeedback(Long tenantId,
                                                 Long recommendationId,
                                                 AiDecisionFeedbackCommand command,
                                                 String actor) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("AI decision feedback command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toFeedbackView(row);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param runDate 时间参数，用于计算窗口、过期或审计时间。
     * @param decisionScope decision scope 参数，用于 insertRun 流程中的校验、计算或对象转换。
     * @param requestedCount requested count 参数，用于 insertRun 流程中的校验、计算或对象转换。
     * @param metadata metadata 参数，用于 insertRun 流程中的校验、计算或对象转换。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param startedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 insertRun 流程生成的业务结果。
     */
    private AiDecisionRunDO insertRun(Long tenantId,
                                      LocalDate runDate,
                                      String decisionScope,
                                      int requestedCount,
                                      Map<String, Object> metadata,
                                      String actor,
                                      LocalDateTime startedAt) {
        // 准备本次处理所需的上下文和中间变量。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedAt(startedAt);
        runMapper.insert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return row;
    }

    /**
     * 执行 finishRun 流程，围绕 finish run 完成校验、计算或结果组装。
     *
     * @param run run 参数，用于 finishRun 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param processed processed 参数，用于 finishRun 流程中的校验、计算或对象转换。
     * @param skipped skipped 参数，用于 finishRun 流程中的校验、计算或对象转换。
     * @param failed failed 参数，用于 finishRun 流程中的校验、计算或对象转换。
     * @param errorMessage error message 参数，用于 finishRun 流程中的校验、计算或对象转换。
     */
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

    /**
     * 执行 features 流程，围绕 features 完成校验、计算或结果组装。
     *
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param runDate 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 features 流程生成的业务结果。
     */
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
                /**
                 * 执行 scale 流程，围绕 scale 完成校验、计算或结果组装。
                 *
                 * @return 返回 scale 流程生成的业务结果。
                 */
                : scale(churn.getChurnProbability());
        String churnBand = churn == null ? "UNKNOWN" : defaultString(churn.getChurnRiskBand(), "UNKNOWN");
        return new DecisionFeatures(featureSnapshot, bestSendHour, churnProbability, churnBand);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param run run 参数，用于 writeRecommendations 流程中的校验、计算或对象转换。
     * @param profile profile 参数，用于 writeRecommendations 流程中的校验、计算或对象转换。
     * @param features features 参数，用于 writeRecommendations 流程中的校验、计算或对象转换。
     * @param consentChannels consent channels 参数，用于 writeRecommendations 流程中的校验、计算或对象转换。
     * @param budgetCap budget cap 参数，用于 writeRecommendations 流程中的校验、计算或对象转换。
     * @param createdAt 时间参数，用于计算窗口、过期或审计时间。
     */
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
                /**
                 * 执行 explanation 流程，围绕 explanation 完成校验、计算或结果组装。
                 *
                 * @param createdAt 时间参数，用于计算窗口、过期或审计时间。
                 * @return 返回 explanation 汇总后的集合、分页或映射视图。
                 */
                fallbackReason, featureMap, explanation("LTV", features), createdAt);
        insertRecommendation(tenantId, run, profile.getUserId(), "NEXT_BEST_ACTION", actionKey(features),
                actionKey(features), null, null, actionScore, confidence, 2, new BigDecimal("0.0000"), "ELIGIBLE",
                /**
                 * 执行 explanation 流程，围绕 explanation 完成校验、计算或结果组装。
                 *
                 * @param createdAt 时间参数，用于计算窗口、过期或审计时间。
                 * @return 返回 explanation 汇总后的集合、分页或映射视图。
                 */
                fallbackReason, featureMap, explanation("NEXT_BEST_ACTION", features), createdAt);
        BigDecimal offerCost = offerCost(features);
        boolean budgetConstrained = budgetCap != null && offerCost.compareTo(budgetCap) > 0;
        insertRecommendation(tenantId, run, profile.getUserId(), "NEXT_BEST_OFFER", offerKey(features),
                null, offerKey(features), null, offerScore, confidence, 3, offerCost,
                budgetConstrained ? "BUDGET_CONSTRAINED" : "ELIGIBLE",
                budgetConstrained ? "BUDGET_CAP_EXCEEDED" : fallbackReason,
                /**
                 * 执行 explanation 流程，围绕 explanation 完成校验、计算或结果组装。
                 *
                 * @param createdAt 时间参数，用于计算窗口、过期或审计时间。
                 * @return 返回 explanation 汇总后的集合、分页或映射视图。
                 */
                featureMap, explanation("NEXT_BEST_OFFER", features), createdAt);
        String channel = preferredChannel(consentChannels, profile);
        insertRecommendation(tenantId, run, profile.getUserId(), "CHANNEL_AFFINITY", channel,
                null, null, channel, channelScore, confidence, 4, BigDecimal.ZERO, "ELIGIBLE",
                /**
                 * 执行 explanation 流程，围绕 explanation 完成校验、计算或结果组装。
                 *
                 * @param createdAt 时间参数，用于计算窗口、过期或审计时间。
                 * @return 返回 explanation 汇总后的集合、分页或映射视图。
                 */
                fallbackReason, featureMap, explanation("CHANNEL_AFFINITY", features), createdAt);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param run run 参数，用于 insertRecommendation 流程中的校验、计算或对象转换。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param decisionType 类型标识，用于选择对应处理分支。
     * @param decisionKey 业务键，用于在同一租户下定位资源。
     * @param actionKey 业务键，用于在同一租户下定位资源。
     * @param offerKey 业务键，用于在同一租户下定位资源。
     * @param channel channel 参数，用于 insertRecommendation 流程中的校验、计算或对象转换。
     * @param score score 参数，用于 insertRecommendation 流程中的校验、计算或对象转换。
     * @param confidence confidence 参数，用于 insertRecommendation 流程中的校验、计算或对象转换。
     * @param rank rank 参数，用于 insertRecommendation 流程中的校验、计算或对象转换。
     * @param budgetCost budget cost 参数，用于 insertRecommendation 流程中的校验、计算或对象转换。
     * @param eligibilityStatus 业务状态，用于筛选或推进状态流转。
     * @param fallbackReason 原因说明，用于记录状态变化的业务依据。
     * @param features features 参数，用于 insertRecommendation 流程中的校验、计算或对象转换。
     * @param explanation explanation 参数，用于 insertRecommendation 流程中的校验、计算或对象转换。
     * @param createdAt 时间参数，用于计算窗口、过期或审计时间。
     */
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
        // 准备本次处理所需的上下文和中间变量。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedAt(createdAt);
        recommendationMapper.insert(row);
    }

    /**
     * 执行 featureMap 流程，围绕 feature map 完成校验、计算或结果组装。
     *
     * @param profile profile 参数，用于 featureMap 流程中的校验、计算或对象转换。
     * @param features features 参数，用于 featureMap 流程中的校验、计算或对象转换。
     * @param consentChannels consent channels 参数，用于 featureMap 流程中的校验、计算或对象转换。
     * @return 返回 featureMap 流程生成的业务结果。
     */
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

    /**
     * 执行 explanation 流程，围绕 explanation 完成校验、计算或结果组装。
     *
     * @param decisionType 类型标识，用于选择对应处理分支。
     * @param features features 参数，用于 explanation 流程中的校验、计算或对象转换。
     * @return 返回 explanation 流程生成的业务结果。
     */
    private Map<String, Object> explanation(String decisionType, DecisionFeatures features) {
        return Map.of(
                "decisionType", decisionType,
                "contributions", List.of(
                        Map.of("feature", "churnProbability", "value", features.churnProbability()),
                        Map.of("feature", "goalCount30d", "value", features.snapshot().goalCount30d()),
                        Map.of("feature", "bestSendHour", "value", features.bestSendHour())));
    }

    /**
     * 执行 consentChannels 流程，围绕 consent channels 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 consent channels 汇总后的集合、分页或映射视图。
     */
    private List<String> consentChannels(Long tenantId, String userId) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(consentMapper.selectList(new LambdaQueryWrapper<MarketingConsentDO>()
                        .eq(MarketingConsentDO::getTenantId, tenantId)
                        .eq(MarketingConsentDO::getUserId, userId)
                        .eq(MarketingConsentDO::getConsentStatus, MarketingConsentDO.OPT_IN)))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .filter(row -> tenantId.equals(row.getTenantId()))
                .filter(row -> MarketingConsentDO.OPT_IN.equals(row.getConsentStatus()))
                .map(MarketingConsentDO::getChannel)
                .filter(this::hasText)
                .map(channel -> channel.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    /**
     * 执行 actionKey 流程，围绕 action key 完成校验、计算或结果组装。
     *
     * @param features features 参数，用于 actionKey 流程中的校验、计算或对象转换。
     * @return 返回 action key 生成的文本或业务键。
     */
    private String actionKey(DecisionFeatures features) {
        if (features.churnProbability().compareTo(new BigDecimal("0.70000")) >= 0) {
            return "RETENTION_INTERVENTION";
        }
        if (features.snapshot().goalCount30d() > 0) {
            return "LOYALTY_NURTURE";
        }
        return "CROSS_SELL";
    }

    /**
     * 执行 offerKey 流程，围绕 offer key 完成校验、计算或结果组装。
     *
     * @param features features 参数，用于 offerKey 流程中的校验、计算或对象转换。
     * @return 返回 offer key 生成的文本或业务键。
     */
    private String offerKey(DecisionFeatures features) {
        if (features.churnProbability().compareTo(new BigDecimal("0.70000")) >= 0) {
            return "SERVICE_BENEFIT";
        }
        return features.snapshot().goalCount30d() > 0 ? "POINTS_BONUS" : "NO_OFFER";
    }

    /**
     * 执行 offerCost 流程，围绕 offer cost 完成校验、计算或结果组装。
     *
     * @param features features 参数，用于 offerCost 流程中的校验、计算或对象转换。
     * @return 返回 offer cost 计算得到的数量、金额或指标值。
     */
    private BigDecimal offerCost(DecisionFeatures features) {
        if (features.churnProbability().compareTo(new BigDecimal("0.70000")) >= 0) {
            return new BigDecimal("25.0000");
        }
        return new BigDecimal("5.0000");
    }

    /**
     * 执行 preferredChannel 流程，围绕 preferred channel 完成校验、计算或结果组装。
     *
     * @param consentChannels consent channels 参数，用于 preferredChannel 流程中的校验、计算或对象转换。
     * @param profile profile 参数，用于 preferredChannel 流程中的校验、计算或对象转换。
     * @return 返回 preferred channel 生成的文本或业务键。
     */
    private String preferredChannel(List<String> consentChannels, CdpUserProfileDO profile) {
        if (consentChannels.contains("EMAIL") && hasText(profile.getEmail())) {
            return "EMAIL";
        }
        if (consentChannels.contains("SMS") && hasText(profile.getPhone())) {
            return "SMS";
        }
        return "IN_APP";
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private AiDecisionRecommendationView toRecommendationView(AiUserDecisionRecommendationDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param requestedUsers requested users 参数，用于 boundedUsers 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private List<String> boundedUsers(List<String> requestedUsers) {
        Set<String> users = new LinkedHashSet<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String userId : requestedUsers == null ? List.<String>of() : requestedUsers) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (hasText(userId)) {
                users.add(userId.trim());
            }
        }
        int limit = Math.max(1, properties.getBatchSize());
        return new ArrayList<>(users).stream().limit(limit).toList();
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
            throw new IllegalArgumentException("AI decision JSON serialization failed", ex);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String json) {
        if (!hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 score 计算得到的数量、金额或指标值。
     */
    private BigDecimal score(double value) {
        double bounded = Math.max(0.0d, Math.min(1.0d, value));
        return BigDecimal.valueOf(bounded).setScale(5, RoundingMode.HALF_UP);
    }

    /**
     * 执行 scale 流程，围绕 scale 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 scale 计算得到的数量、金额或指标值。
     */
    private BigDecimal scale(BigDecimal value) {
        return value.setScale(5, RoundingMode.HALF_UP);
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
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 normalizeUpper 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeUpper(String value, String fallback) {
        return defaultString(value, fallback).toUpperCase(Locale.ROOT);
    }

    /**
     * 执行 optionalUpper 流程，围绕 optional upper 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 optional upper 生成的文本或业务键。
     */
    private String optionalUpper(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 default int 计算得到的数量、金额或指标值。
     */
    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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

    /**
     * DecisionFeatures 数据记录。
     */
    private record DecisionFeatures(
            ChurnFeatureSnapshotService.FeatureSnapshot snapshot,
            int bestSendHour,
            BigDecimal churnProbability,
            String churnBand) {
    }
}
