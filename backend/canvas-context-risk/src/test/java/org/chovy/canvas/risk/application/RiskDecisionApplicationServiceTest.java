package org.chovy.canvas.risk.application;

import org.chovy.canvas.risk.api.RiskDecisionCommand;
import org.chovy.canvas.risk.domain.dsl.RiskRuleConditionNode;
import org.chovy.canvas.risk.domain.dsl.RiskRuleGroupNode;
import org.chovy.canvas.risk.domain.dsl.RiskRuleLogic;
import org.chovy.canvas.risk.domain.dsl.RiskRuleOperand;
import org.chovy.canvas.risk.domain.dsl.RiskRuleOperator;
import org.chovy.canvas.risk.domain.dsl.RiskRuntimeMode;
import org.chovy.canvas.risk.domain.runtime.RiskActiveStrategyReader;
import org.chovy.canvas.risk.domain.runtime.RiskCompiledRule;
import org.chovy.canvas.risk.domain.runtime.RiskCompiledStrategy;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionAction;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionLedger;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionMerger;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionRequest;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionRuleHit;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionRunRecord;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionService;
import org.chovy.canvas.risk.domain.runtime.RiskFailPolicy;
import org.chovy.canvas.risk.domain.runtime.RiskRequestFeatureResolver;
import org.chovy.canvas.risk.domain.runtime.RiskResolvedValue;
import org.chovy.canvas.risk.domain.runtime.RiskRuleEvaluator;
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

class RiskDecisionApplicationServiceTest {

    @Test
    void evaluateMapsCommandToDomainDecisionView() {
        RiskDecisionApplicationService service = new RiskDecisionApplicationService(domainService());

        var view = service.evaluate(new RiskDecisionCommand(
                10L,
                "req-api-1",
                "MARKETING_BENEFIT_ISSUE",
                Instant.parse("2026-06-08T09:59:00Z"),
                map("userId", "user-123"),
                map("amount", 100),
                map("caller", "CANVAS_NODE"),
                map("risk.score", 90),
                50));

        assertThat(view.requestId()).isEqualTo("req-api-1");
        assertThat(view.decision()).isEqualTo("BLOCK");
        assertThat(view.riskBand()).isEqualTo("HIGH");
        assertThat(view.matchedRules()).containsExactly("velocity:score-high");
    }

    private RiskDecisionService domainService() {
        RiskCompiledStrategy strategy = new RiskCompiledStrategy(
                "MARKETING_BENEFIT_ISSUE",
                "benefit_default",
                12,
                RiskRuntimeMode.ENFORCE,
                RiskFailPolicy.FAIL_REVIEW,
                List.of("risk.score"),
                List.of(new RiskCompiledRule(
                        "velocity",
                        "score-high",
                        new RiskRuleGroupNode(RiskRuleLogic.AND, List.of(new RiskRuleConditionNode(
                                RiskRuleOperand.feature("risk.score"),
                                RiskRuleOperator.GTE,
                                RiskRuleOperand.literal(85))), List.of()),
                        RiskDecisionAction.BLOCK,
                        90,
                        "score-high",
                        false)));
        return new RiskDecisionService(
                (tenantId, sceneKey) -> strategy,
                new FakeLedger(),
                (request, operand) -> RiskResolvedValue.present(request.suppliedFeatures().get("risk.score")),
                new RiskRuleEvaluator(),
                new RiskDecisionMerger(),
                Clock.fixed(Instant.parse("2026-06-08T10:00:00Z"), ZoneOffset.UTC));
    }

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            result.put((String) pairs[index], pairs[index + 1]);
        }
        return result;
    }

    private static final class FakeLedger implements RiskDecisionLedger {
        private final List<RiskDecisionRunRecord> savedRuns = new ArrayList<>();

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
        }
    }
}
