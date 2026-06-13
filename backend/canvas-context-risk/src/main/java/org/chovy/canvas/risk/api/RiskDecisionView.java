package org.chovy.canvas.risk.api;

import java.util.List;

public record RiskDecisionView(
        String requestId,
        String decisionRunId,
        String sceneKey,
        String strategyKey,
        int strategyVersion,
        String mode,
        String decision,
        int score,
        String riskBand,
        List<String> reasons,
        List<String> matchedRules,
        List<String> labels,
        List<String> missingFeatures,
        boolean traceAvailable,
        int latencyMs) {
}
