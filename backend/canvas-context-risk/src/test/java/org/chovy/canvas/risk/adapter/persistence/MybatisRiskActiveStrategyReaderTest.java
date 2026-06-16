package org.chovy.canvas.risk.adapter.persistence;

import org.chovy.canvas.risk.adapter.external.JacksonRiskRuleJsonCodec;
import org.chovy.canvas.risk.domain.dsl.RiskRuntimeMode;
import org.chovy.canvas.risk.domain.runtime.RiskCompiledStrategy;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionAction;
import org.chovy.canvas.risk.domain.runtime.RiskFailPolicy;
import org.chovy.canvas.risk.domain.runtime.RiskStrategyCompiler;
import org.chovy.canvas.risk.domain.runtime.RiskStrategyRuntimeCache;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MybatisRiskActiveStrategyReaderTest {

    @Test
    void compilesActiveStrategyWithCaseInsensitiveEnumValues() {
        RiskStrategyMapper strategyMapper = mock(RiskStrategyMapper.class);
        RiskStrategyVersionMapper versionMapper = mock(RiskStrategyVersionMapper.class);
        RiskStrategyDO strategy = new RiskStrategyDO();
        strategy.setTenantId(7L);
        strategy.setSceneKey("payment");
        strategy.setStrategyKey("payment-risk");
        strategy.setStatus("ACTIVE");
        strategy.setActiveVersion(3);
        strategy.setRiskLevel("HIGH");
        RiskStrategyVersionDO version = new RiskStrategyVersionDO();
        version.setTenantId(7L);
        version.setStrategyKey("payment-risk");
        version.setVersion(3);
        version.setMode("enforce");
        version.setTrafficPercent(BigDecimal.valueOf(100));
        version.setDefinitionJson("""
                {
                  "mode": "enforce",
                  "failPolicy": "fail_review",
                  "groups": [{
                    "groupKey": "default",
                    "groupType": "HARD_RULE",
                    "executionOrder": 0,
                    "matchPolicy": "ANY_MATCHED",
                    "enabled": true,
                    "rules": [{
                      "ruleKey": "amount-high",
                      "mode": "enforce",
                      "dsl": {
                        "logic": "AND",
                        "conditions": [{
                          "left": {"type": "EVENT", "path": "amount"},
                          "op": ">=",
                          "right": {"type": "LITERAL", "value": 100}
                        }],
                        "groups": []
                      },
                      "action": "BLOCK",
                      "scoreDelta": 80,
                      "reasonCode": "AMOUNT_HIGH"
                    }]
                  }]
                }
                """);
        when(strategyMapper.selectList(any())).thenReturn(List.of(strategy));
        when(versionMapper.selectOne(any())).thenReturn(version);

        MybatisRiskActiveStrategyReader reader = new MybatisRiskActiveStrategyReader(
                strategyMapper,
                versionMapper,
                new RiskStrategyRuntimeCache(new RiskStrategyCompiler(new JacksonRiskRuleJsonCodec())),
                null);

        RiskCompiledStrategy compiled = reader.findActiveStrategy(7L, "payment");

        assertThat(compiled.mode()).isEqualTo(RiskRuntimeMode.ENFORCE);
        assertThat(compiled.failPolicy()).isEqualTo(RiskFailPolicy.FAIL_REVIEW);
        assertThat(compiled.rules()).hasSize(1);
        assertThat(compiled.rules().getFirst().action()).isEqualTo(RiskDecisionAction.BLOCK);
    }
}
