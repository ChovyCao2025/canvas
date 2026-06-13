package org.chovy.canvas.risk.domain.runtime;

import java.util.List;

/**
 * 风控策略规则组定义。
 *
 * @param groupKey 规则组键
 * @param groupType 规则组类型
 * @param executionOrder 执行顺序
 * @param matchPolicy 命中策略
 * @param enabled 是否启用
 * @param rules 规则列表
 */
public record RiskStrategyRuleGroupDefinition(
        String groupKey,
        String groupType,
        int executionOrder,
        String matchPolicy,
        boolean enabled,
        List<RiskStrategyRuleDefinition> rules
) {

    public RiskStrategyRuleGroupDefinition {
        rules = rules == null ? List.of() : List.copyOf(rules);
    }
}
