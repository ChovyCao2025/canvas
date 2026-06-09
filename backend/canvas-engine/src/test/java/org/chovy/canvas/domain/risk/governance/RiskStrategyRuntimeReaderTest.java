package org.chovy.canvas.domain.risk.governance;

import org.chovy.canvas.domain.risk.runtime.RiskActiveStrategyReader;
import org.chovy.canvas.domain.risk.runtime.RiskCompiledStrategy;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionAction;
import org.chovy.canvas.web.risk.RiskStrategyRuntimeCache;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskStrategyRuntimeReaderTest {

    private final RecordingRuntimeCache runtimeCache = new RecordingRuntimeCache();
    private final RiskStrategyService service = new RiskStrategyService(
            (tenantId, eventType, strategyKey, version, actor) -> {
            },
            runtimeCache);

    @Test
    void activeStrategyCanBeReadAsCompiledRuntimeStrategyByScene() {
        service.createDraft(7L, new RiskStrategyCommand(
                "payment",
                "payment-risk",
                "Payment risk",
                "LOW",
                compilerCompatibleDefinitionJson()), "alice");
        service.validate(7L, "payment-risk", 1, "alice");
        service.activate(7L, "payment-risk", 1, "alice");

        RiskCompiledStrategy compiled = ((RiskActiveStrategyReader) service).findActiveStrategy(7L, "payment");

        assertThat(compiled.strategyKey()).isEqualTo("payment-risk");
        assertThat(compiled.version()).isEqualTo(1);
        assertThat(compiled.requiredFeatures()).containsExactly("risk.score");
        assertThat(compiled.rules()).hasSize(1);
        assertThat(compiled.rules().getFirst().action()).isEqualTo(RiskDecisionAction.BLOCK);
        assertThat(compiled.compiledHash()).startsWith("sha256:");
        assertThat(runtimeCache.invalidations).containsExactly("7:payment-risk");
    }

    @Test
    void inactiveOrDifferentTenantStrategiesAreNotVisibleToRuntimeReader() {
        service.createDraft(7L, new RiskStrategyCommand(
                "payment",
                "payment-risk",
                "Payment risk",
                "LOW",
                compilerCompatibleDefinitionJson()), "alice");
        service.validate(7L, "payment-risk", 1, "alice");

        assertThat(((RiskActiveStrategyReader) service).findActiveStrategy(7L, "payment")).isNull();
        service.activate(7L, "payment-risk", 1, "alice");
        assertThat(((RiskActiveStrategyReader) service).findActiveStrategy(8L, "payment")).isNull();
    }

    private String compilerCompatibleDefinitionJson() {
        return """
                {
                  "mode": "ENFORCE",
                  "failPolicy": "FAIL_REVIEW",
                  "latencyBudgetMs": 50,
                  "groups": [
                    {
                      "groupKey": "velocity",
                      "groupType": "HARD_RULE",
                      "executionOrder": 10,
                      "matchPolicy": "ANY_MATCHED",
                      "enabled": true,
                      "rules": [
                        {
                          "ruleKey": "score-high",
                          "priority": 100,
                          "mode": "ENFORCE",
                          "action": "BLOCK",
                          "scoreDelta": 90,
                          "reasonCode": "score-high",
                          "dsl": {
                            "logic": "AND",
                            "conditions": [
                              {
                                "left": {"type": "FEATURE", "key": "risk.score"},
                                "op": ">=",
                                "right": {"type": "LITERAL", "value": 85}
                              }
                            ],
                            "groups": []
                          }
                        }
                      ]
                    }
                  ]
                }
                """;
    }

    private static final class RecordingRuntimeCache implements RiskStrategyRuntimeCache {
        private final List<String> invalidations = new ArrayList<>();

        @Override
        public void invalidate(Long tenantId, String strategyKey) {
            invalidations.add(tenantId + ":" + strategyKey);
        }
    }
}
