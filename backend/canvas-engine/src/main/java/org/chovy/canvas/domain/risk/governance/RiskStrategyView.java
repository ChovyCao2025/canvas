package org.chovy.canvas.domain.risk.governance;

/**
 * 风控策略聚合视图。
 *
 * @param tenantId 租户编号
 * @param sceneKey 场景业务键
 * @param strategyKey 策略业务键
 * @param name 策略名称
 * @param status 当前状态
 * @param activeVersion 活跃版本号
 * @param draftVersion 草稿版本号
 * @param riskLevel 风险等级
 * @param owner 负责人
 */
public record RiskStrategyView(
        Long tenantId,
        String sceneKey,
        String strategyKey,
        String name,
        RiskStrategyLifecycleStatus status,
        Integer activeVersion,
        Integer draftVersion,
        String riskLevel,
        String owner
) {
}
