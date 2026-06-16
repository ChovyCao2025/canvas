package org.chovy.canvas.risk.domain.runtime;

import org.chovy.canvas.risk.domain.dsl.RiskRuleGroupNode;
import java.util.Objects;

/**
 * 已编译的单条风控规则。
 *
 * @param groupKey 所属规则组键
 * @param ruleKey 规则键
 * @param rule 解析后的规则 DSL 节点
 * @param action 规则命中后的建议动作
 * @param scoreDelta 规则命中后的分数增量
 * @param reasonCode 命中原因码
 * @param shadowRule 是否为影子规则
 */
public final class RiskCompiledRule {

    /**
     * RiskCompiledRule 的 groupKey 字段。
     */
    private final String groupKey;


    /**
     * RiskCompiledRule 的 ruleKey 字段。
     */
    private final String ruleKey;


    /**
     * RiskCompiledRule 的 rule 字段。
     */
    private final RiskRuleGroupNode rule;


    /**
     * RiskCompiledRule 的 action 字段。
     */
    private final RiskDecisionAction action;


    /**
     * RiskCompiledRule 的 scoreDelta 字段。
     */
    private final int scoreDelta;


    /**
     * RiskCompiledRule 的 reasonCode 字段。
     */
    private final String reasonCode;


    /**
     * RiskCompiledRule 的 shadowRule 字段。
     */
    private final boolean shadowRule;


    /**
     * 创建 RiskCompiledRule。
     *
     * @param groupKey RiskCompiledRule 的 groupKey 字段
     * @param ruleKey RiskCompiledRule 的 ruleKey 字段
     * @param rule RiskCompiledRule 的 rule 字段
     * @param action RiskCompiledRule 的 action 字段
     * @param scoreDelta RiskCompiledRule 的 scoreDelta 字段
     * @param reasonCode RiskCompiledRule 的 reasonCode 字段
     * @param shadowRule RiskCompiledRule 的 shadowRule 字段
     */
    public RiskCompiledRule(String groupKey, String ruleKey, RiskRuleGroupNode rule, RiskDecisionAction action, int scoreDelta, String reasonCode, boolean shadowRule) {
        this.groupKey = groupKey;
        this.ruleKey = ruleKey;
        this.rule = rule;
        this.action = action;
        this.scoreDelta = scoreDelta;
        this.reasonCode = reasonCode;
        this.shadowRule = shadowRule;
    }

    /**
     * 返回 RiskCompiledRule 的 groupKey 字段。
     *
     * @return groupKey 字段值
     */
    public String groupKey() {
        return groupKey;
    }

    /**
     * 返回 RiskCompiledRule 的 ruleKey 字段。
     *
     * @return ruleKey 字段值
     */
    public String ruleKey() {
        return ruleKey;
    }

    /**
     * 返回 RiskCompiledRule 的 rule 字段。
     *
     * @return rule 字段值
     */
    public RiskRuleGroupNode rule() {
        return rule;
    }

    /**
     * 返回 RiskCompiledRule 的 action 字段。
     *
     * @return action 字段值
     */
    public RiskDecisionAction action() {
        return action;
    }

    /**
     * 返回 RiskCompiledRule 的 scoreDelta 字段。
     *
     * @return scoreDelta 字段值
     */
    public int scoreDelta() {
        return scoreDelta;
    }

    /**
     * 返回 RiskCompiledRule 的 reasonCode 字段。
     *
     * @return reasonCode 字段值
     */
    public String reasonCode() {
        return reasonCode;
    }

    /**
     * 返回 RiskCompiledRule 的 shadowRule 字段。
     *
     * @return shadowRule 字段值
     */
    public boolean shadowRule() {
        return shadowRule;
    }

    /**
     * 比较当前 RiskCompiledRule 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskCompiledRule other)) {
            return false;
        }
        return Objects.equals(groupKey, other.groupKey)
                && Objects.equals(ruleKey, other.ruleKey)
                && Objects.equals(rule, other.rule)
                && Objects.equals(action, other.action)
                && scoreDelta == other.scoreDelta
                && Objects.equals(reasonCode, other.reasonCode)
                && shadowRule == other.shadowRule;
    }

    /**
     * 计算 RiskCompiledRule 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupKey, ruleKey, rule, action, scoreDelta, reasonCode, shadowRule);
    }

    /**
     * 返回 RiskCompiledRule 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskCompiledRule[groupKey=" + groupKey + ", ruleKey=" + ruleKey + ", rule=" + rule + ", action=" + action + ", scoreDelta=" + scoreDelta + ", reasonCode=" + reasonCode + ", shadowRule=" + shadowRule + "]";
    }

    /**
         * 返回影子规则副本。
         */
        public RiskCompiledRule shadow() {
            return new RiskCompiledRule(groupKey, ruleKey, rule, action, scoreDelta, reasonCode, true);
        }
}
