package org.chovy.canvas.domain.risk.lab;

/**
 * 风控仿真请求。
 *
 * @param tenantId 租户编号
 * @param sceneKey 场景业务键
 * @param strategyKey 策略业务键
 * @param baselineVersion 基线版本号
 * @param candidateVersion 候选版本号
 * @param sampleLimit 样本数量上限
 */
public record RiskSimulationRequest(
        Long tenantId,
        String sceneKey,
        String strategyKey,
        int baselineVersion,
        int candidateVersion,
        int sampleLimit
) {
}
