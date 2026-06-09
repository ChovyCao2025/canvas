package org.chovy.canvas.web.risk;

/**
 * 风控场景延迟预算提供者。
 */
@FunctionalInterface
public interface RiskSceneBudgetProvider {

    /**
     * 返回指定租户和场景允许的最大决策耗时。
     */
    int latencyBudgetMs(Long tenantId, String sceneKey);
}
