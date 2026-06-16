package org.chovy.canvas.risk.domain.runtime;

import org.chovy.canvas.risk.domain.dsl.RiskRuntimeMode;

import java.util.List;
import java.util.Objects;

/**
 * 风控策略规则定义。
 *
 * @param ruleKey 规则键
 * @param priority 规则优先级
 * @param mode 规则运行模式
 * @param dslJson 规则 DSL JSON
 * @param action 命中动作
 * @param scoreDelta 分数增量
 * @param reasonCode 原因码
 * @param labels 规则标签
 */
public final class RiskStrategyRuleDefinition {

    /**
     * RiskStrategyRuleDefinition 的 ruleKey 字段。
     */
    private final String ruleKey;


    /**
     * RiskStrategyRuleDefinition 的 priority 字段。
     */
    private final int priority;


    /**
     * RiskStrategyRuleDefinition 的 mode 字段。
     */
    private final RiskRuntimeMode mode;


    /**
     * RiskStrategyRuleDefinition 的 dslJson 字段。
     */
    private final String dslJson;


    /**
     * RiskStrategyRuleDefinition 的 action 字段。
     */
    private final String action;


    /**
     * RiskStrategyRuleDefinition 的 scoreDelta 字段。
     */
    private final int scoreDelta;


    /**
     * RiskStrategyRuleDefinition 的 reasonCode 字段。
     */
    private final String reasonCode;


    /**
     * RiskStrategyRuleDefinition 的 labels 字段。
     */
    private final List<String> labels;


    /**
     * 创建 RiskStrategyRuleDefinition。
     *
     * @param ruleKey RiskStrategyRuleDefinition 的 ruleKey 字段
     * @param priority RiskStrategyRuleDefinition 的 priority 字段
     * @param mode RiskStrategyRuleDefinition 的 mode 字段
     * @param dslJson RiskStrategyRuleDefinition 的 dslJson 字段
     * @param action RiskStrategyRuleDefinition 的 action 字段
     * @param scoreDelta RiskStrategyRuleDefinition 的 scoreDelta 字段
     * @param reasonCode RiskStrategyRuleDefinition 的 reasonCode 字段
     * @param labels RiskStrategyRuleDefinition 的 labels 字段
     */
    public RiskStrategyRuleDefinition(String ruleKey, int priority, RiskRuntimeMode mode, String dslJson, String action, int scoreDelta, String reasonCode, List<String> labels) {
        labels = labels == null ? List.of() : List.copyOf(labels);
        this.ruleKey = ruleKey;
        this.priority = priority;
        this.mode = mode;
        this.dslJson = dslJson;
        this.action = action;
        this.scoreDelta = scoreDelta;
        this.reasonCode = reasonCode;
        this.labels = labels;
    }

    /**
     * 返回 RiskStrategyRuleDefinition 的 ruleKey 字段。
     *
     * @return ruleKey 字段值
     */
    public String ruleKey() {
        return ruleKey;
    }

    /**
     * 返回 RiskStrategyRuleDefinition 的 priority 字段。
     *
     * @return priority 字段值
     */
    public int priority() {
        return priority;
    }

    /**
     * 返回 RiskStrategyRuleDefinition 的 mode 字段。
     *
     * @return mode 字段值
     */
    public RiskRuntimeMode mode() {
        return mode;
    }

    /**
     * 返回 RiskStrategyRuleDefinition 的 dslJson 字段。
     *
     * @return dslJson 字段值
     */
    public String dslJson() {
        return dslJson;
    }

    /**
     * 返回 RiskStrategyRuleDefinition 的 action 字段。
     *
     * @return action 字段值
     */
    public String action() {
        return action;
    }

    /**
     * 返回 RiskStrategyRuleDefinition 的 scoreDelta 字段。
     *
     * @return scoreDelta 字段值
     */
    public int scoreDelta() {
        return scoreDelta;
    }

    /**
     * 返回 RiskStrategyRuleDefinition 的 reasonCode 字段。
     *
     * @return reasonCode 字段值
     */
    public String reasonCode() {
        return reasonCode;
    }

    /**
     * 返回 RiskStrategyRuleDefinition 的 labels 字段。
     *
     * @return labels 字段值
     */
    public List<String> labels() {
        return labels;
    }

    /**
     * 比较当前 RiskStrategyRuleDefinition 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskStrategyRuleDefinition other)) {
            return false;
        }
        return Objects.equals(ruleKey, other.ruleKey)
                && priority == other.priority
                && Objects.equals(mode, other.mode)
                && Objects.equals(dslJson, other.dslJson)
                && Objects.equals(action, other.action)
                && scoreDelta == other.scoreDelta
                && Objects.equals(reasonCode, other.reasonCode)
                && Objects.equals(labels, other.labels);
    }

    /**
     * 计算 RiskStrategyRuleDefinition 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(ruleKey, priority, mode, dslJson, action, scoreDelta, reasonCode, labels);
    }

    /**
     * 返回 RiskStrategyRuleDefinition 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskStrategyRuleDefinition[ruleKey=" + ruleKey + ", priority=" + priority + ", mode=" + mode + ", dslJson=" + dslJson + ", action=" + action + ", scoreDelta=" + scoreDelta + ", reasonCode=" + reasonCode + ", labels=" + labels + "]";
    }
}
