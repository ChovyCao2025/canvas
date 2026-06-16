package org.chovy.canvas.risk.domain.runtime;

import java.util.Objects;

/**
 * 风控规则命中明细。
 *
 * @param groupKey 规则组键
 * @param ruleKey 规则键
 * @param action 命中动作
 * @param scoreDelta 分数增量
 * @param reasonCode 原因码
 * @param shadow 是否影子命中
 */
public final class RiskDecisionRuleHit {

    /**
     * RiskDecisionRuleHit 的 groupKey 字段。
     */
    private final String groupKey;


    /**
     * RiskDecisionRuleHit 的 ruleKey 字段。
     */
    private final String ruleKey;


    /**
     * RiskDecisionRuleHit 的 action 字段。
     */
    private final RiskDecisionAction action;


    /**
     * RiskDecisionRuleHit 的 scoreDelta 字段。
     */
    private final int scoreDelta;


    /**
     * RiskDecisionRuleHit 的 reasonCode 字段。
     */
    private final String reasonCode;


    /**
     * RiskDecisionRuleHit 的 shadow 字段。
     */
    private final boolean shadow;


    /**
     * 创建 RiskDecisionRuleHit。
     *
     * @param groupKey RiskDecisionRuleHit 的 groupKey 字段
     * @param ruleKey RiskDecisionRuleHit 的 ruleKey 字段
     * @param action RiskDecisionRuleHit 的 action 字段
     * @param scoreDelta RiskDecisionRuleHit 的 scoreDelta 字段
     * @param reasonCode RiskDecisionRuleHit 的 reasonCode 字段
     * @param shadow RiskDecisionRuleHit 的 shadow 字段
     */
    public RiskDecisionRuleHit(String groupKey, String ruleKey, RiskDecisionAction action, int scoreDelta, String reasonCode, boolean shadow) {
        this.groupKey = groupKey;
        this.ruleKey = ruleKey;
        this.action = action;
        this.scoreDelta = scoreDelta;
        this.reasonCode = reasonCode;
        this.shadow = shadow;
    }

    /**
     * 返回 RiskDecisionRuleHit 的 groupKey 字段。
     *
     * @return groupKey 字段值
     */
    public String groupKey() {
        return groupKey;
    }

    /**
     * 返回 RiskDecisionRuleHit 的 ruleKey 字段。
     *
     * @return ruleKey 字段值
     */
    public String ruleKey() {
        return ruleKey;
    }

    /**
     * 返回 RiskDecisionRuleHit 的 action 字段。
     *
     * @return action 字段值
     */
    public RiskDecisionAction action() {
        return action;
    }

    /**
     * 返回 RiskDecisionRuleHit 的 scoreDelta 字段。
     *
     * @return scoreDelta 字段值
     */
    public int scoreDelta() {
        return scoreDelta;
    }

    /**
     * 返回 RiskDecisionRuleHit 的 reasonCode 字段。
     *
     * @return reasonCode 字段值
     */
    public String reasonCode() {
        return reasonCode;
    }

    /**
     * 返回 RiskDecisionRuleHit 的 shadow 字段。
     *
     * @return shadow 字段值
     */
    public boolean shadow() {
        return shadow;
    }

    /**
     * 比较当前 RiskDecisionRuleHit 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskDecisionRuleHit other)) {
            return false;
        }
        return Objects.equals(groupKey, other.groupKey)
                && Objects.equals(ruleKey, other.ruleKey)
                && Objects.equals(action, other.action)
                && scoreDelta == other.scoreDelta
                && Objects.equals(reasonCode, other.reasonCode)
                && shadow == other.shadow;
    }

    /**
     * 计算 RiskDecisionRuleHit 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupKey, ruleKey, action, scoreDelta, reasonCode, shadow);
    }

    /**
     * 返回 RiskDecisionRuleHit 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskDecisionRuleHit[groupKey=" + groupKey + ", ruleKey=" + ruleKey + ", action=" + action + ", scoreDelta=" + scoreDelta + ", reasonCode=" + reasonCode + ", shadow=" + shadow + "]";
    }
}
