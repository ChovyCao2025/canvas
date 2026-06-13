package org.chovy.canvas.risk.api;

public record RiskSceneView(
        Long tenantId,
        String sceneKey,
        String displayName,
        String eventSchemaKey,
        String status,
        String defaultMode,
        String failPolicy,
        Integer latencyBudgetMs,
        String owner) {
}
