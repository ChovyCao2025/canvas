package org.chovy.canvas.risk.domain.runtime;

/**
 * 定义 RiskDecisionMetrics 的风控模块职责和数据契约。
 */
public interface RiskDecisionMetrics {

    /**
     * 执行 recordRiskDecision 相关的风控处理逻辑。
     */
    void recordRiskDecision(String sceneKey, String action, int latencyMs);

    /**
     * 执行 recordRiskRuleHit 相关的风控处理逻辑。
     */
    void recordRiskRuleHit(String sceneKey, String groupKey, String ruleKey, String action);

    /**
     * 执行 recordRiskFeatureMissing 相关的风控处理逻辑。
     */
    void recordRiskFeatureMissing(String sceneKey, String featureKey);

    /**
     * 执行 recordRiskDecisionFailure 相关的风控处理逻辑。
     */
    void recordRiskDecisionFailure(String sceneKey, String errorType);
}
