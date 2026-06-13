package org.chovy.canvas.risk.domain.runtime;

public interface RiskDecisionMetrics {

    void recordRiskDecision(String sceneKey, String action, int latencyMs);

    void recordRiskRuleHit(String sceneKey, String groupKey, String ruleKey, String action);

    void recordRiskFeatureMissing(String sceneKey, String featureKey);

    void recordRiskDecisionFailure(String sceneKey, String errorType);
}
