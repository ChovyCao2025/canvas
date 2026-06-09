package org.chovy.canvas.domain.risk.lab;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 内存风控仿真运行仓储，供测试和无数据库环境使用。
 */
public class InMemoryRiskSimulationRunRepository implements RiskSimulationRunRepository {

    private final List<RiskSimulationHistoryView> runs = new ArrayList<>();

    @Override
    public void save(RiskSimulationRequest request, RiskSimulationResult result) {
        runs.add(0, new RiskSimulationHistoryView(
                result.simulationId(),
                request.sceneKey(),
                request.strategyKey(),
                request.baselineVersion(),
                request.candidateVersion(),
                result.status(),
                result.sampleSize(),
                result.actionDistribution(),
                result.changedActionCount(),
                result.actionChanges(),
                Instant.now().toString()));
    }

    @Override
    public List<RiskSimulationHistoryView> list(Long tenantId, String sceneKey, int limit) {
        return runs.stream()
                .filter(run -> sceneKey == null || sceneKey.isBlank() || Objects.equals(run.sceneKey(), sceneKey))
                .limit(normalizedLimit(limit))
                .toList();
    }

    private int normalizedLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 100);
    }
}
