package org.chovy.canvas.domain.risk.runtime;

import org.chovy.canvas.dal.dataobject.RiskDecisionRunDO;
import org.chovy.canvas.dal.dataobject.RiskRuleHitDO;
import org.chovy.canvas.dal.mapper.RiskDecisionRunMapper;
import org.chovy.canvas.dal.mapper.RiskRuleHitMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcRiskDecisionTraceReaderTest {

    private final RiskDecisionRunMapper runMapper = mock(RiskDecisionRunMapper.class);
    private final RiskRuleHitMapper hitMapper = mock(RiskRuleHitMapper.class);
    private final JdbcRiskDecisionTraceReader reader = new JdbcRiskDecisionTraceReader(runMapper, hitMapper);

    @Test
    void readsRecentDecisionRunsWithRuleHits() {
        RiskDecisionRunDO run = new RiskDecisionRunDO();
        run.setId(42L);
        run.setTenantId(7L);
        run.setRequestId("risk-req-1");
        run.setSceneKey("MARKETING_BENEFIT_ISSUE");
        run.setStrategyKey("benefit_default");
        run.setStrategyVersion(12);
        run.setMode("ENFORCE");
        run.setDecision("BLOCK");
        run.setScore(90);
        run.setRiskBand("HIGH");
        run.setLatencyMs(9);
        run.setCreatedAt(LocalDateTime.parse("2026-06-08T10:00:00"));
        RiskRuleHitDO hit = new RiskRuleHitDO();
        hit.setDecisionRunId(42L);
        hit.setGroupKey("velocity");
        hit.setRuleKey("block");
        when(runMapper.selectList(any())).thenReturn(List.of(run));
        when(hitMapper.selectList(any())).thenReturn(List.of(hit));

        assertThat(reader.listTraces(7L, "MARKETING_BENEFIT_ISSUE", 20))
                .singleElement()
                .satisfies(trace -> {
                    assertThat(trace.traceId()).isEqualTo("42");
                    assertThat(trace.requestId()).isEqualTo("risk-req-1");
                    assertThat(trace.decision()).isEqualTo("BLOCK");
                    assertThat(trace.matchedRules()).containsExactly("velocity:block");
                    assertThat(trace.createdAt()).isEqualTo("2026-06-08T10:00:00Z");
                });
    }
}
