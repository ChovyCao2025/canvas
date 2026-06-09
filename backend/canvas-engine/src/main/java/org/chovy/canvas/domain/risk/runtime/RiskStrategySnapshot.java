package org.chovy.canvas.domain.risk.runtime;

import org.chovy.canvas.domain.risk.dsl.RiskRuntimeMode;

import java.util.List;
import java.util.Map;

/**
 * 持久化策略版本快照，作为编译器输入和运行时缓存身份。
 *
 * @param tenantId 租户编号
 * @param sceneKey 场景业务键
 * @param strategyKey 策略业务键
 * @param version 策略版本号
 * @param mode 运行模式
 * @param trafficPercent 流量比例
 * @param failPolicy 失败策略
 * @param latencyBudgetMs 延迟预算
 * @param groups 规则组列表
 * @param metadata 策略元数据
 */
public record RiskStrategySnapshot(
        Long tenantId,
        String sceneKey,
        String strategyKey,
        int version,
        RiskRuntimeMode mode,
        int trafficPercent,
        RiskFailPolicy failPolicy,
        int latencyBudgetMs,
        List<RiskStrategyRuleGroupDefinition> groups,
        Map<String, Object> metadata
) {

    public RiskStrategySnapshot {
        groups = groups == null ? List.of() : List.copyOf(groups);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * 返回运行时缓存键。
     */
    public String cacheKey() {
        return tenantId + ":" + sceneKey + ":" + strategyKey + ":" + version;
    }
}
