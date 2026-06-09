package org.chovy.canvas.domain.risk.lab;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.RiskSimulationRunDO;
import org.chovy.canvas.dal.mapper.RiskSimulationRunMapper;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionAction;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcRiskSimulationRunRepositoryTest {

    private final RiskSimulationRunMapper mapper = mock(RiskSimulationRunMapper.class);
    private final JdbcRiskSimulationRunRepository repository = new JdbcRiskSimulationRunRepository(
            mapper,
            new ObjectMapper(),
            Clock.fixed(Instant.parse("2026-06-09T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void persistsAndReadsSimulationRunHistory() {
        when(mapper.selectOne(any())).thenReturn(null);
        RiskSimulationRequest request = new RiskSimulationRequest(
                7L, "MARKETING_BENEFIT_ISSUE", "benefit_default", 1, 2, 50);
        RiskSimulationResult result = new RiskSimulationResult(
                "sim-1",
                RiskSimulationStatus.COMPLETED,
                10,
                Map.of(RiskDecisionAction.REVIEW, 10),
                2,
                Map.of("REVIEW->BLOCK", 2));

        repository.save(request, result);

        ArgumentCaptor<RiskSimulationRunDO> captor = ArgumentCaptor.forClass(RiskSimulationRunDO.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getSimulationId()).isEqualTo("sim-1");
        assertThat(captor.getValue().getActionDistributionJson()).contains("REVIEW");
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(LocalDateTime.parse("2026-06-09T00:00"));

        RiskSimulationRunDO row = captor.getValue();
        when(mapper.selectList(any())).thenReturn(List.of(row));

        assertThat(repository.list(7L, "MARKETING_BENEFIT_ISSUE", 20))
                .singleElement()
                .satisfies(view -> {
                    assertThat(view.simulationId()).isEqualTo("sim-1");
                    assertThat(view.actionDistribution()).containsEntry(RiskDecisionAction.REVIEW, 10);
                    assertThat(view.actionChanges()).containsEntry("REVIEW->BLOCK", 2);
                });
    }
}
