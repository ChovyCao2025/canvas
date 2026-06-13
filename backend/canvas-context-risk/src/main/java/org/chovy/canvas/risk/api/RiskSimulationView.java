package org.chovy.canvas.risk.api;

import java.util.Map;

public record RiskSimulationView(
        String simulationId,
        String status,
        int sampleSize,
        Map<String, Integer> actionDistribution,
        int changedActionCount,
        Map<String, Integer> actionChanges) {
}
