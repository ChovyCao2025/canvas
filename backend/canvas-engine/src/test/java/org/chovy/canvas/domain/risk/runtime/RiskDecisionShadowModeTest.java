package org.chovy.canvas.domain.risk.runtime;

import org.chovy.canvas.domain.risk.dsl.RiskRuleConditionNode;
import org.chovy.canvas.domain.risk.dsl.RiskRuleGroupNode;
import org.chovy.canvas.domain.risk.dsl.RiskRuleLogic;
import org.chovy.canvas.domain.risk.dsl.RiskRuleOperand;
import org.chovy.canvas.domain.risk.dsl.RiskRuleOperator;
import org.chovy.canvas.domain.risk.dsl.RiskRuntimeMode;
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

class RiskDecisionShadowModeTest {

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

    @Test
    void shadowRecordsSuggestedDecisionButReturnsBaselineAllow() {
        strategyReader.strategy = strategy(RiskRuntimeMode.SHADOW);
        featureResolver.values.put(RiskRuleOperand.feature("risk.score"), RiskResolvedValue.present(90));

        RiskDecisionResponse response = service.evaluate(request("shadow", "user-1"));

        assertThat(response.action()).isEqualTo(RiskDecisionAction.ALLOW);
        assertThat(response.labels()).contains("baseline:ALLOW", "candidate:BLOCK", "mode:SHADOW");
        assertThat(ledger.savedHits).extracting(RiskDecisionRuleHit::ruleKey).containsExactly("score-high");
    }

    @Test
    void dualRunRecordsBaselineAndCandidateResult() {
        strategyReader.strategy = strategy(RiskRuntimeMode.DUAL_RUN);
        featureResolver.values.put(RiskRuleOperand.feature("risk.score"), RiskResolvedValue.present(90));

        RiskDecisionResponse response = service.evaluate(request("dual", "user-1"));

        assertThat(response.action()).isEqualTo(RiskDecisionAction.ALLOW);
        assertThat(response.labels()).contains("baseline:ALLOW", "candidate:BLOCK", "mode:DUAL_RUN");
        assertThat(response.traceAvailable()).isTrue();
    }

    @Test
    void canaryChoosesCandidateByDeterministicSubjectHash() {
        strategyReader.strategy = strategy(RiskRuntimeMode.CANARY);
        featureResolver.values.put(RiskRuleOperand.feature("risk.score"), RiskResolvedValue.present(90));

        RiskDecisionResponse first = service.evaluate(request("canary-1", "stable-user"));
        RiskDecisionResponse second = service.evaluate(request("canary-2", "stable-user"));

        assertThat(second.action()).isEqualTo(first.action());
        assertThat(second.labels()).contains(first.labels().stream()
                .filter(label -> label.startsWith("canary:"))
                .findFirst()
                .orElseThrow());
    }

    @Test
    void markExecutesCandidateAndNeverChangesReturnedAction() {
        strategyReader.strategy = strategy(RiskRuntimeMode.MARK);
        featureResolver.values.put(RiskRuleOperand.feature("risk.score"), RiskResolvedValue.present(90));

        RiskDecisionResponse response = service.evaluate(request("mark", "user-1"));

        assertThat(response.action()).isEqualTo(RiskDecisionAction.ALLOW);
        assertThat(response.labels()).contains("baseline:ALLOW", "candidate:BLOCK", "mode:MARK");
        assertThat(ledger.savedHits).extracting(RiskDecisionRuleHit::ruleKey).containsExactly("score-high");
    }

    @Test
    void traceReplayCanReconstructBaselineAndCandidateFromPersistedResponse() {
        strategyReader.strategy = strategy(RiskRuntimeMode.DUAL_RUN);
        featureResolver.values.put(RiskRuleOperand.feature("risk.score"), RiskResolvedValue.present(90));

        service.evaluate(request("trace", "user-1"));

        RiskDecisionResponse persisted = ledger.savedRuns.getFirst().response();
        assertThat(persisted.labels()).contains("baseline:ALLOW", "candidate:BLOCK", "mode:DUAL_RUN");
        assertThat(persisted.matchedRules()).contains("velocity:score-high");
        assertThat(persisted.traceAvailable()).isTrue();
    }

    private RiskDecisionRequest request(String requestId, String userId) {
        return new RiskDecisionRequest(
                10L,
                requestId,
                "MARKETING_BENEFIT_ISSUE",
                Instant.parse("2026-06-08T09:59:00Z"),
                orderedMap("amount", 100, "channel", "APP"),
                orderedMap("userId", userId),
                orderedMap("caller", "CANVAS_NODE"),
                orderedMap("risk.score", 90),
                50);
    }

    private RiskCompiledStrategy strategy(RiskRuntimeMode mode) {
        return new RiskCompiledStrategy(
                "MARKETING_BENEFIT_ISSUE",
                "benefit_default",
                12,
                mode,
                RiskFailPolicy.FAIL_REVIEW,
                List.of("risk.score"),
                List.of(rule("score-high")));
    }

    private RiskCompiledRule rule(String ruleKey) {
        return new RiskCompiledRule(
                "velocity",
                ruleKey,
                new RiskRuleGroupNode(RiskRuleLogic.AND,
                        List.of(new RiskRuleConditionNode(
                                RiskRuleOperand.feature("risk.score"),
                                RiskRuleOperator.GTE,
                                RiskRuleOperand.literal(85))),
                        List.of()),
                RiskDecisionAction.BLOCK,
                90,
                ruleKey,
                false);
    }

    private Map<String, Object> orderedMap(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }

    private static final class FakeStrategyReader implements RiskActiveStrategyReader {
        private RiskCompiledStrategy strategy;

        @Override
        public RiskCompiledStrategy findActiveStrategy(Long tenantId, String sceneKey) {
            return strategy;
        }
    }

    private static final class FakeLedger implements RiskDecisionLedger {
        private final List<RiskDecisionRunRecord> savedRuns = new ArrayList<>();
        private final List<RiskDecisionRuleHit> savedHits = new ArrayList<>();

        @Override
        public Optional<RiskDecisionRunRecord> findByRequest(Long tenantId, String requestId) {
            return savedRuns.stream()
                    .filter(run -> run.tenantId().equals(tenantId) && run.requestId().equals(requestId))
                    .findFirst();
        }

        @Override
        public RiskDecisionRunRecord saveRun(RiskDecisionRunRecord run) {
            RiskDecisionRunRecord saved = run.withDecisionRunId("rd-" + (savedRuns.size() + 1));
            savedRuns.add(saved);
            return saved;
        }

        @Override
        public void saveRuleHits(String decisionRunId, List<RiskDecisionRuleHit> hits) {
            savedHits.addAll(hits);
        }
    }

    private static final class RecordingFeatureResolver implements RiskRequestFeatureResolver {
        private final Map<RiskRuleOperand, RiskResolvedValue> values = new LinkedHashMap<>();

        @Override
        public RiskResolvedValue resolve(RiskDecisionRequest request, RiskRuleOperand operand) {
            return values.getOrDefault(operand, RiskResolvedValue.missing());
        }
    }
}
