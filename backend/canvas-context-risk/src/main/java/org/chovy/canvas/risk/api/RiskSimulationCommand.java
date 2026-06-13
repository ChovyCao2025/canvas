package org.chovy.canvas.risk.api;

public record RiskSimulationCommand(
        Long tenantId,
        String sceneKey,
        String strategyKey,
        int baselineVersion,
        int candidateVersion,
        int sampleLimit) {
}
