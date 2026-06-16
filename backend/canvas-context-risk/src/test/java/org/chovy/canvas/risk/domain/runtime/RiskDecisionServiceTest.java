package org.chovy.canvas.risk.domain.runtime;

import org.chovy.canvas.risk.domain.dsl.RiskRuleConditionNode;
import org.chovy.canvas.risk.domain.dsl.RiskRuleGroupNode;
import org.chovy.canvas.risk.domain.dsl.RiskRuleLogic;
import org.chovy.canvas.risk.domain.dsl.RiskRuleOperand;
import org.chovy.canvas.risk.domain.dsl.RiskRuleOperator;
import org.chovy.canvas.risk.domain.dsl.RiskRuntimeMode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * 定义 RiskDecisionServiceTest 的风控模块职责和数据契约。
 */
class RiskDecisionServiceTest {

    private final FakeStrategyReader strategyReader = new FakeStrategyReader();
    private final FakeLedger ledger = new FakeLedger();
    private final RecordingFeatureResolver featureResolver = new RecordingFeatureResolver();
    private final RiskDecisionService service = new RiskDecisionService(
            strategyReader,
            ledger,
            featureResolver,
            new RiskRuleEvaluator(),
            new RiskDecisionMerger(),
            Clock.fixed(Instant.parse("2026-06-08T10:00:00Z"), ZoneOffset.UTC));


    /**
     * 执行 evaluatesActiveStrategyForScene 相关的风控处理逻辑。
     */
    @Test
    void evaluatesActiveStrategyForScene() {
        strategyReader.strategy = strategyWithRules(rule("score-high",
                RiskRuleOperand.feature("risk.score"),
                RiskRuleOperator.GTE,
                RiskRuleOperand.literal(85),
                RiskDecisionAction.BLOCK,
                90));
        featureResolver.values.put(RiskRuleOperand.feature("risk.score"), RiskResolvedValue.present(90));

        RiskDecisionResponse response = service.evaluate(request("req-1"));

        assertThat(response.action()).isEqualTo(RiskDecisionAction.BLOCK);
        assertThat(response.score()).isEqualTo(90);
        assertThat(response.riskBand()).isEqualTo(RiskBand.HIGH);
        assertThat(response.matchedRules()).containsExactly("velocity:score-high");
    }

    /**
     * 执行 repeatedRequestIdWithSameCanonicalPayloadReturnsPersistedDecision 相关的风控处理逻辑。
     */
    @Test
    void repeatedRequestIdWithSameCanonicalPayloadReturnsPersistedDecision() {
        strategyReader.strategy = strategyWithRules(rule("score-high",
                RiskRuleOperand.feature("risk.score"),
                RiskRuleOperator.GTE,
                RiskRuleOperand.literal(85),
                RiskDecisionAction.BLOCK,
                90));
        featureResolver.values.put(RiskRuleOperand.feature("risk.score"), RiskResolvedValue.present(90));

        RiskDecisionResponse first = service.evaluate(request("same-req"));
        RiskDecisionResponse second = service.evaluate(request("same-req"));

        assertThat(second).isEqualTo(first);
        assertThat(ledger.savedRuns).hasSize(1);
    }

    /**
     * 执行 repeatedRequestIdWithDifferentPayloadThrowsReplayMismatch 相关的风控处理逻辑。
     */
    @Test
    void repeatedRequestIdWithDifferentPayloadThrowsReplayMismatch() {
        strategyReader.strategy = strategyWithRules();
        service.evaluate(request("same-req"));

        RiskDecisionRequest changed = request("same-req").withEvent(Map.of("amount", 999));

        assertThatExceptionOfType(RiskDecisionReplayMismatchException.class)
                .isThrownBy(() -> service.evaluate(changed));
    }

    /**
     * 执行 featureResolverReceivesRequiredFeaturesOnly 相关的风控处理逻辑。
     */
    @Test
    void featureResolverReceivesRequiredFeaturesOnly() {
        strategyReader.strategy = strategyWithRules(
                rule("score-high", RiskRuleOperand.feature("risk.score"), RiskRuleOperator.GTE,
                        RiskRuleOperand.literal(85), RiskDecisionAction.BLOCK, 90)
        ).withRequiredFeatures(List.of("risk.score"));
        featureResolver.values.put(RiskRuleOperand.feature("risk.score"), RiskResolvedValue.present(90));
        featureResolver.values.put(RiskRuleOperand.feature("unused.feature"), RiskResolvedValue.present(1));

        service.evaluate(request("req-features"));

        assertThat(featureResolver.resolvedFeatureKeys).containsExactly("risk.score");
    }

    /**
     * 执行 persistsDecisionRunBeforeReturning 相关的风控处理逻辑。
     */
    @Test
    void persistsDecisionRunBeforeReturning() {
        strategyReader.strategy = strategyWithRules();

        RiskDecisionResponse response = service.evaluate(request("persist-me"));

        assertThat(ledger.savedRuns).hasSize(1);
        assertThat(ledger.savedRuns.getFirst().requestId()).isEqualTo("persist-me");
        assertThat(ledger.savedRuns.getFirst().response()).isEqualTo(response);
    }

    /**
     * 执行 persistsRuleHitsForEffectiveAndShadowSignals 相关的风控处理逻辑。
     */
    @Test
    void persistsRuleHitsForEffectiveAndShadowSignals() {
        strategyReader.strategy = strategyWithRules(
                rule("score-high", RiskRuleOperand.feature("risk.score"), RiskRuleOperator.GTE,
                        RiskRuleOperand.literal(85), RiskDecisionAction.BLOCK, 90),
                rule("shadow-review", RiskRuleOperand.feature("risk.shadow"), RiskRuleOperator.GTE,
                        RiskRuleOperand.literal(1), RiskDecisionAction.REVIEW, 20).shadow()
        ).withRequiredFeatures(List.of("risk.score", "risk.shadow"));
        featureResolver.values.put(RiskRuleOperand.feature("risk.score"), RiskResolvedValue.present(90));
        featureResolver.values.put(RiskRuleOperand.feature("risk.shadow"), RiskResolvedValue.present(1));

        service.evaluate(request("hits"));

        assertThat(ledger.savedHits).extracting(RiskDecisionRuleHit::ruleKey)
                .containsExactly("score-high", "shadow-review");
        assertThat(ledger.savedHits).extracting(RiskDecisionRuleHit::shadow).containsExactly(false, true);
    }

    /**
     * 执行 appliesFailOpenWhenRuntimeDependencyFails 相关的风控处理逻辑。
     */
    @Test
    void appliesFailOpenWhenRuntimeDependencyFails() {
        strategyReader.strategy = strategyWithRules().withFailPolicy(RiskFailPolicy.FAIL_OPEN);
        featureResolver.fail = true;

        RiskDecisionResponse response = service.evaluate(request("fail-open"));

        assertThat(response.action()).isEqualTo(RiskDecisionAction.ALLOW);
        assertThat(response.reasons()).contains("RUNTIME_FAILURE:feature resolver failed");
    }

    /**
     * 执行 appliesFailReviewWhenRuntimeDependencyFails 相关的风控处理逻辑。
     */
    @Test
    void appliesFailReviewWhenRuntimeDependencyFails() {
        strategyReader.strategy = strategyWithRules().withFailPolicy(RiskFailPolicy.FAIL_REVIEW);
        featureResolver.fail = true;

        RiskDecisionResponse response = service.evaluate(request("fail-review"));

        assertThat(response.action()).isEqualTo(RiskDecisionAction.REVIEW);
    }

    /**
     * 执行 appliesFailClosedWhenRuntimeDependencyFails 相关的风控处理逻辑。
     */
    @Test
    void appliesFailClosedWhenRuntimeDependencyFails() {
        strategyReader.strategy = strategyWithRules().withFailPolicy(RiskFailPolicy.FAIL_CLOSED);
        featureResolver.fail = true;

        RiskDecisionResponse response = service.evaluate(request("fail-closed"));

        assertThat(response.action()).isEqualTo(RiskDecisionAction.BLOCK);
    }

    /**
     * 执行 deadlineExceededUsesSceneFailPolicy 相关的风控处理逻辑。
     */
    @Test
    void deadlineExceededUsesSceneFailPolicy() {
        strategyReader.strategy = strategyWithRules().withFailPolicy(RiskFailPolicy.FAIL_CLOSED);

        RiskDecisionResponse response = service.evaluate(request("deadline").withDeadlineMs(0));

        assertThat(response.action()).isEqualTo(RiskDecisionAction.BLOCK);
        assertThat(response.reasons()).contains("RUNTIME_FAILURE:deadline exceeded");
    }

    /**
     * 执行 inputSnapshotMasksRawPiiBeforePersistence 相关的风控处理逻辑。
     */
    @Test
    void inputSnapshotMasksRawPiiBeforePersistence() {
        strategyReader.strategy = strategyWithRules();

        service.evaluate(request("pii"));

        String snapshot = ledger.savedRuns.getFirst().inputSnapshotJson();
        assertThat(ledger.savedRuns.getFirst().subjectHash())
                .startsWith("sha256:")
                .doesNotContain("user-123", "user@example.com", "+15551234567");
        assertThat(snapshot).doesNotContain("user@example.com");
        assertThat(snapshot).doesNotContain("+15551234567");
        assertThat(snapshot).contains("u***3");
        assertThat(snapshot).contains("***4567");
    }

    /**
     * 执行 request 相关的风控处理逻辑。
     */
    private RiskDecisionRequest request(String requestId) {
        return new RiskDecisionRequest(
                10L,
                requestId,
                "MARKETING_BENEFIT_ISSUE",
                Instant.parse("2026-06-08T09:59:00Z"),
                orderedMap("amount", 100, "channel", "APP"),
                orderedMap("userId", "user-123", "email", "user@example.com", "phone", "+15551234567"),
                orderedMap("caller", "CANVAS_NODE", "canvasId", 42),
                orderedMap("risk.score", 90),
                50);
    }

    /**
     * 执行 strategyWithRules 相关的风控处理逻辑。
     */
    private RiskCompiledStrategy strategyWithRules(RiskCompiledRule... rules) {
        return new RiskCompiledStrategy(
                "MARKETING_BENEFIT_ISSUE",
                "benefit_default",
                12,
                RiskRuntimeMode.ENFORCE,
                RiskFailPolicy.FAIL_REVIEW,
                List.of("risk.score"),
                List.of(rules));
    }

    /**
     * 执行 rule 相关的风控处理逻辑。
     */
    private RiskCompiledRule rule(String ruleKey,
                                  RiskRuleOperand left,
                                  RiskRuleOperator operator,
                                  RiskRuleOperand right,
                                  RiskDecisionAction action,
                                  int scoreDelta) {
        return new RiskCompiledRule(
                "velocity",
                ruleKey,
                new RiskRuleGroupNode(RiskRuleLogic.AND,
                        List.of(new RiskRuleConditionNode(left, operator, right)),
                        List.of()),
                action,
                scoreDelta,
                ruleKey,
                false);
    }

    /**
     * 执行 orderedMap 相关的风控处理逻辑。
     */
    private Map<String, Object> orderedMap(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }

    /**
     * 定义 FakeStrategyReader 的风控模块职责和数据契约。
     */
    private static final class FakeStrategyReader implements RiskActiveStrategyReader {
        /**
         * 保存 strategy 对应的风控状态或配置。
         */
        private RiskCompiledStrategy strategy;


        /**
         * 执行 findActiveStrategy 相关的风控处理逻辑。
         */
        @Override
        public RiskCompiledStrategy findActiveStrategy(Long tenantId, String sceneKey) {
            return strategy;
        }
    }

    /**
     * 定义 FakeLedger 的风控模块职责和数据契约。
     */
    private static final class FakeLedger implements RiskDecisionLedger {
        private final List<RiskDecisionRunRecord> savedRuns = new ArrayList<>();
        private final List<RiskDecisionRuleHit> savedHits = new ArrayList<>();


        /**
         * 执行 findByRequest 相关的风控处理逻辑。
         */
        @Override
        public Optional<RiskDecisionRunRecord> findByRequest(Long tenantId, String requestId) {
            return savedRuns.stream()
                    .filter(run -> run.tenantId().equals(tenantId) && run.requestId().equals(requestId))
                    .findFirst();
        }

        /**
         * 执行 saveRun 相关的风控处理逻辑。
         */
        @Override
        public RiskDecisionRunRecord saveRun(RiskDecisionRunRecord run) {
            RiskDecisionRunRecord saved = run.withDecisionRunId("rd-" + (savedRuns.size() + 1));
            savedRuns.add(saved);
            return saved;
        }

        /**
         * 执行 saveRuleHits 相关的风控处理逻辑。
         */
        @Override
        public void saveRuleHits(String decisionRunId, List<RiskDecisionRuleHit> hits) {
            savedHits.addAll(hits);
        }
    }

    /**
     * 定义 RecordingFeatureResolver 的风控模块职责和数据契约。
     */
    private static final class RecordingFeatureResolver implements RiskRequestFeatureResolver {
        private final Map<RiskRuleOperand, RiskResolvedValue> values = new LinkedHashMap<>();
        private final List<String> resolvedFeatureKeys = new ArrayList<>();

        /**
         * 保存 fail 对应的风控状态或配置。
         */
        private boolean fail;


        /**
         * 执行 resolve 相关的风控处理逻辑。
         */
        @Override
        public RiskResolvedValue resolve(RiskDecisionRequest request, RiskRuleOperand operand) {
            if (fail) {
                throw new IllegalStateException("feature resolver failed");
            }
            if (operand instanceof RiskRuleOperand.FeatureOperand feature) {
                resolvedFeatureKeys.add(feature.key());
            }
            return values.getOrDefault(operand, RiskResolvedValue.missing());
        }
    }
}
