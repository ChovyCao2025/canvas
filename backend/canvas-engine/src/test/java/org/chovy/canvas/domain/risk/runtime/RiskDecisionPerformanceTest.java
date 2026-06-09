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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RiskDecisionPerformanceTest {

    @Test
    void evaluatesHundredRuleStrategyWithinFiftyMillisecondP95Budget() {
        RiskDecisionService service = new RiskDecisionService(
                new FixedStrategyReader(strategyWithRules(20, 5)),
                new InMemoryLedger(),
                (request, operand) -> RiskResolvedValue.present(90),
                new RiskRuleEvaluator(),
                new RiskDecisionMerger(),
                Clock.systemUTC());

        for (int i = 0; i < 25; i++) {
            service.evaluate(request("warmup-" + i));
        }

        List<Long> durationsNanos = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            long started = System.nanoTime();
            RiskDecisionResponse response = service.evaluate(request("measure-" + i));
            durationsNanos.add(System.nanoTime() - started);
            assertThat(response.matchedRules()).hasSize(100);
        }

        Collections.sort(durationsNanos);
        double p95Ms = durationsNanos.get((int) Math.floor(durationsNanos.size() * 0.95) - 1) / 1_000_000.0;

        assertThat(p95Ms).isLessThan(50.0);
    }

    private RiskCompiledStrategy strategyWithRules(int groups, int rulesPerGroup) {
        List<RiskCompiledRule> rules = new ArrayList<>();
        for (int group = 0; group < groups; group++) {
            for (int rule = 0; rule < rulesPerGroup; rule++) {
                rules.add(rule("group-" + group, "rule-" + group + "-" + rule));
            }
        }
        return new RiskCompiledStrategy(
                "MARKETING_BENEFIT_ISSUE",
                "benefit-performance",
                1,
                RiskRuntimeMode.ENFORCE,
                RiskFailPolicy.FAIL_REVIEW,
                List.of("risk.score"),
                rules);
    }

    private RiskCompiledRule rule(String groupKey, String ruleKey) {
        return new RiskCompiledRule(
                groupKey,
                ruleKey,
                new RiskRuleGroupNode(RiskRuleLogic.AND,
                        List.of(new RiskRuleConditionNode(
                                RiskRuleOperand.feature("risk.score"),
                                RiskRuleOperator.GTE,
                                RiskRuleOperand.literal(50))),
                        List.of()),
                RiskDecisionAction.REVIEW,
                1,
                ruleKey,
                false);
    }

    private RiskDecisionRequest request(String requestId) {
        return new RiskDecisionRequest(
                7L,
                requestId,
                "MARKETING_BENEFIT_ISSUE",
                Instant.parse("2026-06-09T00:00:00Z"),
                orderedMap("amount", 100, "channel", "APP"),
                orderedMap("userId", "user-1"),
                orderedMap("caller", "PERFORMANCE_TEST"),
                orderedMap("risk.score", 90),
                50);
    }

    private Map<String, Object> orderedMap(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }

    private record FixedStrategyReader(RiskCompiledStrategy strategy) implements RiskActiveStrategyReader {
        @Override
        public RiskCompiledStrategy findActiveStrategy(Long tenantId, String sceneKey) {
            return strategy;
        }
    }

    private static final class InMemoryLedger implements RiskDecisionLedger {
        private final List<RiskDecisionRunRecord> runs = new ArrayList<>();

        @Override
        public Optional<RiskDecisionRunRecord> findByRequest(Long tenantId, String requestId) {
            return runs.stream()
                    .filter(run -> run.tenantId().equals(tenantId) && run.requestId().equals(requestId))
                    .findFirst();
        }

        @Override
        public RiskDecisionRunRecord saveRun(RiskDecisionRunRecord run) {
            RiskDecisionRunRecord saved = run.withDecisionRunId("perf-" + (runs.size() + 1));
            runs.add(saved);
            return saved;
        }

        @Override
        public void saveRuleHits(String decisionRunId, List<RiskDecisionRuleHit> hits) {
        }
    }
}
