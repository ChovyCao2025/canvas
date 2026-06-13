package org.chovy.canvas.risk.api;

public record RiskStrategyCommand(
        String sceneKey,
        String strategyKey,
        String name,
        String riskLevel,
        String definitionJson) {

    public RiskStrategyCommand withDefinitionJson(String newDefinitionJson) {
        return new RiskStrategyCommand(sceneKey, strategyKey, name, riskLevel, newDefinitionJson);
    }
}
