package org.chovy.canvas.risk.api;

public record RiskStrategyView(
        Long tenantId,
        String sceneKey,
        String strategyKey,
        String name,
        String status,
        Integer activeVersion,
        Integer draftVersion,
        String riskLevel,
        String owner) {
}
