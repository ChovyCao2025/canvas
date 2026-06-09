package org.chovy.canvas.domain.risk.lab;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.RiskDecisionRunDO;
import org.chovy.canvas.dal.mapper.RiskDecisionRunMapper;
import org.chovy.canvas.domain.risk.dsl.RiskRuntimeMode;
import org.chovy.canvas.domain.risk.runtime.RiskBand;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionAction;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionResponse;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionRunRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcRiskSimulationSampleRepositoryTest {

    private final RiskDecisionRunMapper runMapper = mock(RiskDecisionRunMapper.class);
    private final JdbcRiskSimulationSampleRepository repository =
            new JdbcRiskSimulationSampleRepository(runMapper, new ObjectMapper());

    @Test
    void findsTenantScopedSceneSamplesFromPersistedDecisionRuns() {
        when(runMapper.selectList(any())).thenReturn(List.of(
                row(101L, "req-1", RiskDecisionAction.ALLOW),
                row(102L, "req-2", RiskDecisionAction.REVIEW)));

        List<RiskDecisionRunRecord> samples = repository.findSamples(7L, "BENEFIT", 50);

        assertThat(samples).hasSize(2);
        assertThat(samples).extracting(RiskDecisionRunRecord::decisionRunId).containsExactly("101", "102");
        assertThat(samples).extracting(RiskDecisionRunRecord::tenantId).containsExactly(7L, 7L);
        assertThat(samples).extracting(RiskDecisionRunRecord::requestId).containsExactly("req-1", "req-2");
        assertThat(samples).extracting(sample -> sample.response().action())
                .containsExactly(RiskDecisionAction.ALLOW, RiskDecisionAction.REVIEW);
        assertThat(samples).extracting(RiskDecisionRunRecord::inputSnapshotJson)
                .allSatisfy(snapshot -> assertThat(snapshot).doesNotContain("user@example.com", "+15551234567"));
        ArgumentCaptor<Wrapper<RiskDecisionRunDO>> queryCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(runMapper).selectList(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getSqlSegment())
                .contains("tenant_id", "scene_key", "LIMIT 50");
    }

    @Test
    void candidateEvaluationFallsBackToPersistedBaselineUntilReplayEvaluatorIsAvailable() {
        RiskDecisionRunRecord sample = repository.findSamplesFromRows(List.of(
                row(101L, "req-1", RiskDecisionAction.BLOCK))).getFirst();

        RiskDecisionAction action = repository.evaluateCandidate(sample, "candidate", 2);

        assertThat(action).isEqualTo(RiskDecisionAction.BLOCK);
    }

    private RiskDecisionRunDO row(Long id, String requestId, RiskDecisionAction action) {
        RiskDecisionResponse response = new RiskDecisionResponse(
                requestId,
                String.valueOf(id),
                "BENEFIT",
                "benefit_default",
                3,
                RiskRuntimeMode.ENFORCE,
                action,
                action == RiskDecisionAction.ALLOW ? 10 : 60,
                action == RiskDecisionAction.ALLOW ? RiskBand.LOW : RiskBand.MEDIUM,
                List.of(action.name()),
                List.of("velocity:score"),
                List.of("mode:ENFORCE"),
                List.of(),
                20,
                true);
        RiskDecisionRunDO row = new RiskDecisionRunDO();
        row.setId(id);
        row.setTenantId(7L);
        row.setRequestId(requestId);
        row.setRequestHash("hash-" + requestId);
        row.setSceneKey("BENEFIT");
        row.setStrategyKey("benefit_default");
        row.setStrategyVersion(3);
        row.setDecision(action.name());
        row.setScore(response.score());
        row.setRiskBand(response.riskBand().name());
        row.setMode(response.mode().name());
        row.setLatencyMs(response.latencyMs());
        row.setInputSnapshotJson("{\"subject\":{\"email\":\"u***@example.com\",\"phone\":\"***4567\"}}");
        row.setOutputJson(writeJson(response));
        return row;
    }

    private String writeJson(Object value) {
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }
}
