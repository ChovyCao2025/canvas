package org.chovy.canvas.web.risk.dto;

/**
 * 风控仿真启动请求 DTO。
 *
 * @param tenantId 请求体租户提示，实际租户以认证上下文为准
 * @param sceneKey 场景业务键
 * @param strategyKey 策略业务键
 * @param version 兼容旧客户端的候选版本号
 * @param candidateVersion 候选版本号
 * @param sampleLimit 样本数量上限
 */
public record RiskSimulationStartRequest(
        Long tenantId,
        String sceneKey,
        String strategyKey,
        Integer version,
        Integer candidateVersion,
        Integer sampleLimit
) {
}
