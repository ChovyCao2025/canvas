package org.chovy.canvas.risk.domain.runtime;

import org.chovy.canvas.risk.domain.dsl.RiskRuleOperand;
import org.chovy.canvas.risk.domain.dsl.RiskRuntimeMode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 风控决策服务，使用当前生效策略评估请求，并持久化不可变的决策轨迹。
 */
public class RiskDecisionService {

    private final RiskActiveStrategyReader strategyReader;
    private final RiskDecisionLedger ledger;
    private final RiskRequestFeatureResolver featureResolver;
    private final RiskRuleEvaluator evaluator;
    private final RiskDecisionMerger merger;
    private final Clock clock;
    private final RiskDecisionMetrics metrics;

    /**
     * 构造不带指标采集器的决策服务。
     */
    public RiskDecisionService(RiskActiveStrategyReader strategyReader,
                               RiskDecisionLedger ledger,
                               RiskRequestFeatureResolver featureResolver,
                               RiskRuleEvaluator evaluator,
                               RiskDecisionMerger merger,
                               Clock clock) {
        this(strategyReader, ledger, featureResolver, evaluator, merger, clock, null);
    }

    /**
     * 构造完整决策服务，允许注入时钟和指标采集器以支持测试与运行时观测。
     */
    public RiskDecisionService(RiskActiveStrategyReader strategyReader,
                               RiskDecisionLedger ledger,
                               RiskRequestFeatureResolver featureResolver,
                               RiskRuleEvaluator evaluator,
                               RiskDecisionMerger merger,
                               Clock clock,
                               RiskDecisionMetrics metrics) {
        this.strategyReader = strategyReader;
        this.ledger = ledger;
        this.featureResolver = featureResolver;
        this.evaluator = evaluator;
        this.merger = merger;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.metrics = metrics;
    }

    /**
     * 对外执行一次风控决策；若 requestId 已存在则按幂等规则回放既有结果。
     */
    public RiskDecisionResponse evaluate(RiskDecisionRequest request) {
        Instant startedAt = clock.instant();
        String requestHash = hash(canonicalPayload(request));
        // requestId 是幂等键，payload 哈希用于阻止相同 requestId 携带不同事实数据重放。
        return ledger.findByRequest(request.tenantId(), request.requestId())
                .map(existing -> replayOrThrow(request, requestHash, existing))
                .orElseGet(() -> evaluateNew(request, requestHash, startedAt));
    }

    /**
     * 校验已存在记录的请求哈希，匹配时返回历史响应，不匹配时拒绝重放。
     */
    private RiskDecisionResponse replayOrThrow(RiskDecisionRequest request,
                                               String requestHash,
                                               RiskDecisionRunRecord existing) {
        if (!existing.requestHash().equals(requestHash)) {
            throw new RiskDecisionReplayMismatchException(request.requestId());
        }
        return existing.response();
    }

    /**
     * 对首次出现的请求执行完整策略评估、信号合并和结果落库。
     */
    private RiskDecisionResponse evaluateNew(RiskDecisionRequest request, String requestHash, Instant startedAt) {
        RiskCompiledStrategy strategy = strategyReader.findActiveStrategy(request.tenantId(), request.sceneKey());
        if (strategy == null) {
            // 场景未配置策略时仍记录一次可追踪决策，并按 FAIL_REVIEW 返回人工复核。
            strategy = new RiskCompiledStrategy(request.sceneKey(), "missing", 0,
                    null, RiskFailPolicy.FAIL_REVIEW, List.of(), List.of());
        }
        try {
            if (request.deadlineMs() <= 0) {
                throw new RiskRuntimeDependencyException("deadline exceeded");
            }
            Map<String, RiskResolvedValue> requiredFeatureValues = resolveRequiredFeatures(request, strategy);
            List<RiskDecisionSignal> signals = new ArrayList<>();
            List<RiskDecisionRuleHit> hits = new ArrayList<>();
            List<String> matchedRules = new ArrayList<>();
            List<String> missingFeatures = new ArrayList<>();
            for (RiskCompiledRule rule : strategy.rules()) {
                RiskRuleEvaluationResult result = evaluator.evaluate(rule.rule(),
                        operand -> resolveOperand(request, operand, requiredFeatureValues));
                missingFeatures.addAll(result.missingFeatures());
                if (result.matched()) {
                    // 规则命中会转为合并信号；影子规则只进入轨迹，不直接改变强制动作。
                    RiskDecisionSignal signal = RiskDecisionSignal
                            .effective("rule:" + rule.groupKey(), rule.reasonCode(), rule.action(), rule.scoreDelta())
                            .withLabel(rule.groupKey() + ":" + rule.ruleKey());
                    if (rule.shadowRule()) {
                        signal = signal.shadowOnly();
                    }
                    signals.add(signal);
                    hits.add(new RiskDecisionRuleHit(rule.groupKey(), rule.ruleKey(), rule.action(),
                            rule.scoreDelta(), rule.reasonCode(), rule.shadowRule()));
                    matchedRules.add(rule.groupKey() + ":" + rule.ruleKey());
                }
            }
            RiskMergedDecision merged = merger.merge(RiskDecisionMergeRequest.enforce(signals)
                    .withMissingFeatures(missingFeatures)
                    .withFailPolicy(strategy.failPolicy()));
            return persistAndReturn(request, requestHash, strategy, merged, matchedRules,
                    missingFeatures, hits, startedAt);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException error) {
            recordDecisionFailure(request.sceneKey(), error);
            // 运行时依赖失败时按策略失败策略投影结果，不把内部异常直接暴露给调用方。
            RiskMergedDecision merged = merger.merge(RiskDecisionMergeRequest.enforce(List.of())
                    .withMissingFeatures(List.of("RUNTIME_FAILURE:" + error.getMessage()))
                    .withFailPolicy(strategy.failPolicy()));
            return persistAndReturn(request, requestHash, strategy, merged, List.of(),
                    List.of(), List.of(), startedAt);
        }
    }

    /**
     * 根据编译策略声明的必需特征，一次性解析本次请求需要的特征快照。
     */
    private Map<String, RiskResolvedValue> resolveRequiredFeatures(RiskDecisionRequest request,
                                                                   RiskCompiledStrategy strategy) {
        Map<String, RiskResolvedValue> resolved = new LinkedHashMap<>();
        for (String key : strategy.requiredFeatures()) {
            resolved.put(key, featureResolver.resolve(request, RiskRuleOperand.feature(key)));
        }
        return resolved;
    }

    /**
     * 解析规则操作数，优先使用已预取特征，再读取请求事件、主体和上下文字段。
     */
    private RiskResolvedValue resolveOperand(RiskDecisionRequest request,
                                             RiskRuleOperand operand,
                                             Map<String, RiskResolvedValue> requiredFeatureValues) {
        if (operand instanceof RiskRuleOperand.FeatureOperand feature) {
            // 编译阶段收集的必需特征只解析一次，保证同一决策内所有规则看到一致快照。
            return requiredFeatureValues.getOrDefault(feature.key(), RiskResolvedValue.missing());
        }
        if (operand instanceof RiskRuleOperand.LiteralOperand literal) {
            return RiskResolvedValue.present(literal.value());
        }
        if (operand instanceof RiskRuleOperand.EventOperand event) {
            return resolvePath(request.event(), event.path());
        }
        if (operand instanceof RiskRuleOperand.SubjectOperand subject) {
            return resolvePath(request.subject(), subject.path());
        }
        if (operand instanceof RiskRuleOperand.ContextOperand context) {
            return resolvePath(request.context(), context.path());
        }
        return featureResolver.resolve(request, operand);
    }

    /**
     * 从扁平 Map 中按路径读取值，缺失时返回 missing。
     */
    private RiskResolvedValue resolvePath(Map<String, Object> source, String path) {
        if (source.containsKey(path)) {
            return RiskResolvedValue.present(source.get(path));
        }
        return RiskResolvedValue.missing();
    }

    /**
     * 构造公开响应、保存决策运行记录和规则命中证据，并写入指标。
     */
    private RiskDecisionResponse persistAndReturn(RiskDecisionRequest request,
                                                  String requestHash,
                                                  RiskCompiledStrategy strategy,
                                                  RiskMergedDecision merged,
                                                  List<String> matchedRules,
                                                  List<String> missingFeatures,
                                                  List<RiskDecisionRuleHit> hits,
                                                  Instant startedAt) {
        int latencyMs = Math.max(0, (int) Duration.between(startedAt, clock.instant()).toMillis());
        // 运行模式只改变对外强制结果，候选策略证据仍通过 labels 保留下来。
        ProjectedDecision projected = projectByMode(request, strategy, merged);
        RiskDecisionResponse response = new RiskDecisionResponse(
                request.requestId(),
                null,
                request.sceneKey(),
                strategy.strategyKey(),
                strategy.version(),
                strategy.mode(),
                projected.action(),
                projected.score(),
                projected.riskBand(),
                projected.reasons(),
                matchedRules,
                projected.labels(),
                missingFeatures,
                latencyMs,
                !hits.isEmpty());
        RiskDecisionRunRecord saved = ledger.saveRun(new RiskDecisionRunRecord(
                null,
                request.tenantId(),
                request.requestId(),
                requestHash,
                RiskSubjectHashing.sha256(sortedMap(request.subject()).toString()),
                maskedSnapshot(request),
                response));
        ledger.saveRuleHits(saved.decisionRunId(), hits);
        recordDecisionMetrics(request.sceneKey(), response, hits, missingFeatures);
        return saved.response();
    }

    /**
     * 记录决策、规则命中和缺失特征指标。
     */
    private void recordDecisionMetrics(String sceneKey,
                                       RiskDecisionResponse response,
                                       List<RiskDecisionRuleHit> hits,
                                       List<String> missingFeatures) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (metrics == null) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        metrics.recordRiskDecision(sceneKey, response.action().name(), response.latencyMs());
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (RiskDecisionRuleHit hit : hits) {
            metrics.recordRiskRuleHit(sceneKey, hit.groupKey(), hit.ruleKey(), hit.action().name());
        }
        for (String missingFeature : missingFeatures) {
            metrics.recordRiskFeatureMissing(sceneKey, missingFeature);
        }
    }

    /**
     * 记录决策链路失败指标。
     */
    private void recordDecisionFailure(String sceneKey, RuntimeException error) {
        if (metrics != null) {
            metrics.recordRiskDecisionFailure(sceneKey, error.getClass().getSimpleName());
        }
    }

    /**
     * 按运行模式将候选决策投影为最终对外结果。
     */
    private ProjectedDecision projectByMode(RiskDecisionRequest request,
                                            RiskCompiledStrategy strategy,
                                            RiskMergedDecision candidate) {
        RiskRuntimeMode mode = strategy.mode() == null ? RiskRuntimeMode.ENFORCE : strategy.mode();
        return switch (mode) {
            case SHADOW, MARK, DUAL_RUN -> baselineProjection(candidate, mode, null);
            case CANARY -> canaryProjection(request, candidate, mode);
            case PAUSED -> baselineProjection(candidate, mode, "paused:BASELINE");
            case SIMULATION, ENFORCE -> candidateProjection(candidate, mode, null);
        };
    }

    /**
     * 按稳定主体分桶决定灰度请求走候选策略还是基线结果。
     */
    private ProjectedDecision canaryProjection(RiskDecisionRequest request,
                                               RiskMergedDecision candidate,
                                               RiskRuntimeMode mode) {
        boolean candidateSelected = canaryCandidateSelected(request);
        String canaryLabel = candidateSelected ? "canary:CANDIDATE" : "canary:BASELINE";
        return candidateSelected
                /**
                 * 判断业务条件是否成立。
                 *
                 * @param candidate 时间参数，用于计算窗口、过期或审计时间。
                 * @param mode mode 参数，用于 candidateProjection 流程中的校验、计算或对象转换。
                 * @param canaryLabel canary label 参数，用于 candidateProjection 流程中的校验、计算或对象转换。
                 * @return 返回布尔判断结果。
                 */
                ? candidateProjection(candidate, mode, canaryLabel)
                /**
                 * 执行 baselineProjection 流程，围绕 baseline projection 完成校验、计算或结果组装。
                 *
                 * @param candidate 时间参数，用于计算窗口、过期或审计时间。
                 * @param mode mode 参数，用于 baselineProjection 流程中的校验、计算或对象转换。
                 * @param canaryLabel canary label 参数，用于 baselineProjection 流程中的校验、计算或对象转换。
                 * @return 返回 baselineProjection 流程生成的业务结果。
                 */
                : baselineProjection(candidate, mode, canaryLabel);
    }

    /**
     * 将候选合并结果直接投影为对外结果。
     */
    private ProjectedDecision candidateProjection(RiskMergedDecision candidate,
                                                  RiskRuntimeMode mode,
                                                  String extraLabel) {
        return new ProjectedDecision(
                candidate.action(),
                candidate.score(),
                candidate.riskBand(),
                candidate.reasons(),
                replayLabels(candidate, mode, extraLabel));
    }

    /**
     * 将结果投影为基线放行动作，同时保留候选原因和标签。
     */
    private ProjectedDecision baselineProjection(RiskMergedDecision candidate,
                                                 RiskRuntimeMode mode,
                                                 String extraLabel) {
        return new ProjectedDecision(
                RiskDecisionAction.ALLOW,
                0,
                RiskBand.LOW,
                candidate.reasons(),
                replayLabels(candidate, mode, extraLabel));
    }

    /**
     * 生成重放和审计标签，显式记录基线、候选和运行模式。
     */
    private List<String> replayLabels(RiskMergedDecision candidate, RiskRuntimeMode mode, String extraLabel) {
        List<String> labels = new ArrayList<>(candidate.labels());
        // 标签同时保存基线和候选结果，支持重放、审计、影子模式和灰度对比。
        labels.add("baseline:ALLOW");
        labels.add("candidate:" + candidate.action().name());
        labels.add("mode:" + mode.name());
        if (extraLabel != null && !extraLabel.isBlank()) {
            labels.add(extraLabel);
        }
        return List.copyOf(labels);
    }

    /**
     * 使用主体和场景计算稳定灰度桶位。
     */
    private boolean canaryCandidateSelected(RiskDecisionRequest request) {
        String subjectKey = sortedMap(request.subject()).toString();
        String hex = hash(subjectKey + ":" + request.sceneKey());
        long bucket = Long.parseUnsignedLong(hex.substring(0, 8), 16);
        // 稳定主体分桶保证重试和多实例之间的灰度路由一致。
        return bucket % 100 < 50;
    }

    /**
     * 构造用于幂等校验的标准化请求载荷。
     */
    private String canonicalPayload(RiskDecisionRequest request) {
        // 所有 Map 字段按键排序，确保相同事实数据不受插入顺序影响。
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sceneKey", request.sceneKey());
        payload.put("eventTime", request.eventTime() == null ? null : request.eventTime().toString());
        payload.put("subject", sortedMap(request.subject()));
        payload.put("event", sortedMap(request.event()));
        payload.put("context", sortedMap(request.context()));
        payload.put("features", sortedMap(request.suppliedFeatures()));
        return payload.toString();
    }

    /**
     * 返回按键排序的新 Map，用于稳定哈希和快照输出。
     */
    private Map<String, Object> sortedMap(Map<String, Object> source) {
        Map<String, Object> sorted = new LinkedHashMap<>();
        source.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return sorted;
    }

    /**
     * 构造脱敏后的输入快照，供审计和问题排查使用。
     */
    private String maskedSnapshot(RiskDecisionRequest request) {
        // 决策账本保存的是重放/审计快照，不保存原始客户标识。
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("requestId", request.requestId());
        snapshot.put("sceneKey", request.sceneKey());
        snapshot.put("eventTime", request.eventTime());
        snapshot.put("subject", maskSnapshotMap(request.subject()));
        snapshot.put("event", maskSnapshotMap(request.event()));
        snapshot.put("context", maskSnapshotMap(request.context()));
        snapshot.put("features", maskSnapshotMap(request.suppliedFeatures()));
        return snapshot.toString();
    }

    /**
     * 对快照 Map 的每个值执行字段级脱敏。
     */
    private Map<String, Object> maskSnapshotMap(Map<String, Object> subject) {
        Map<String, Object> masked = new LinkedHashMap<>();
        subject.forEach((key, value) -> masked.put(key, maskValue(key, value)));
        return masked;
    }

    /**
     * 根据字段名和值内容执行简单敏感信息脱敏。
     */
    private Object maskValue(String key, Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value == null) {
            return null;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        String text = value.toString();
        String normalizedKey = key == null ? "" : key.toLowerCase();
        if (normalizedKey.contains("email") || text.contains("@")) {
            int index = text.indexOf('@');
            return index <= 1 ? "***" : text.charAt(0) + "***" + text.substring(index);
        }
        if (normalizedKey.contains("phone")) {
            return text.length() <= 4 ? "***" : "***" + text.substring(text.length() - 4);
        }
        if (normalizedKey.contains("userid") || normalizedKey.contains("user_id")) {
            return text.length() <= 2 ? "***" : text.charAt(0) + "***" + text.charAt(text.length() - 1);
        }
        if (normalizedKey.contains("ip")) {
            return "***";
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return "***";
    }

    /**
     * 计算 SHA-256 十六进制哈希。
     */
    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    /**
     * RiskRuntimeDependencyException 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static final class RiskRuntimeDependencyException extends RuntimeException {
        /**
         * 构造运行时依赖异常，用于进入失败策略投影流程。
         */
        private RiskRuntimeDependencyException(String message) {
            super(message);
        }
    }

    /**
     * ProjectedDecision 数据记录。
     */
    private record ProjectedDecision(
            RiskDecisionAction action,
            int score,
            RiskBand riskBand,
            List<String> reasons,
            List<String> labels
    ) {
    }
}
