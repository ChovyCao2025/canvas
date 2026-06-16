package org.chovy.canvas.risk.domain.runtime;

import java.util.List;
import java.util.Objects;

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
public final class RiskStrategyRuleGroupDefinition {

    /**
     * RiskStrategyRuleGroupDefinition 的 groupKey 字段。
     */
    private final String groupKey;


    /**
     * RiskStrategyRuleGroupDefinition 的 groupType 字段。
     */
    private final String groupType;


    /**
     * RiskStrategyRuleGroupDefinition 的 executionOrder 字段。
     */
    private final int executionOrder;


    /**
     * RiskStrategyRuleGroupDefinition 的 matchPolicy 字段。
     */
    private final String matchPolicy;


    /**
     * RiskStrategyRuleGroupDefinition 的 enabled 字段。
     */
    private final boolean enabled;


    /**
     * RiskStrategyRuleGroupDefinition 的 rules 字段。
     */
    private final List<RiskStrategyRuleDefinition> rules;


    /**
     * 创建 RiskStrategyRuleGroupDefinition。
     *
     * @param groupKey RiskStrategyRuleGroupDefinition 的 groupKey 字段
     * @param groupType RiskStrategyRuleGroupDefinition 的 groupType 字段
     * @param executionOrder RiskStrategyRuleGroupDefinition 的 executionOrder 字段
     * @param matchPolicy RiskStrategyRuleGroupDefinition 的 matchPolicy 字段
     * @param enabled RiskStrategyRuleGroupDefinition 的 enabled 字段
     * @param rules RiskStrategyRuleGroupDefinition 的 rules 字段
     */
    public RiskStrategyRuleGroupDefinition(String groupKey, String groupType, int executionOrder, String matchPolicy, boolean enabled, List<RiskStrategyRuleDefinition> rules) {
        rules = rules == null ? List.of() : List.copyOf(rules);
        this.groupKey = groupKey;
        this.groupType = groupType;
        this.executionOrder = executionOrder;
        this.matchPolicy = matchPolicy;
        this.enabled = enabled;
        this.rules = rules;
    }

    /**
     * 返回 RiskStrategyRuleGroupDefinition 的 groupKey 字段。
     *
     * @return groupKey 字段值
     */
    public String groupKey() {
        return groupKey;
    }

    /**
     * 返回 RiskStrategyRuleGroupDefinition 的 groupType 字段。
     *
     * @return groupType 字段值
     */
    public String groupType() {
        return groupType;
    }

    /**
     * 返回 RiskStrategyRuleGroupDefinition 的 executionOrder 字段。
     *
     * @return executionOrder 字段值
     */
    public int executionOrder() {
        return executionOrder;
    }

    /**
     * 返回 RiskStrategyRuleGroupDefinition 的 matchPolicy 字段。
     *
     * @return matchPolicy 字段值
     */
    public String matchPolicy() {
        return matchPolicy;
    }

    /**
     * 返回 RiskStrategyRuleGroupDefinition 的 enabled 字段。
     *
     * @return enabled 字段值
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * 返回 RiskStrategyRuleGroupDefinition 的 rules 字段。
     *
     * @return rules 字段值
     */
    public List<RiskStrategyRuleDefinition> rules() {
        return rules;
    }

    /**
     * 比较当前 RiskStrategyRuleGroupDefinition 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskStrategyRuleGroupDefinition other)) {
            return false;
        }
        return Objects.equals(groupKey, other.groupKey)
                && Objects.equals(groupType, other.groupType)
                && executionOrder == other.executionOrder
                && Objects.equals(matchPolicy, other.matchPolicy)
                && enabled == other.enabled
                && Objects.equals(rules, other.rules);
    }

    /**
     * 计算 RiskStrategyRuleGroupDefinition 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupKey, groupType, executionOrder, matchPolicy, enabled, rules);
    }

    /**
     * 返回 RiskStrategyRuleGroupDefinition 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskStrategyRuleGroupDefinition[groupKey=" + groupKey + ", groupType=" + groupType + ", executionOrder=" + executionOrder + ", matchPolicy=" + matchPolicy + ", enabled=" + enabled + ", rules=" + rules + "]";
    }
}
