package org.chovy.canvas.risk.domain.runtime;

import java.util.List;
import java.util.Objects;

/**
 * 风控规则评估结果。
 *
 * @param matched 规则是否匹配
 * @param evidence 条件级评估证据
 * @param missingFeatures 评估过程中缺失的特征
 */
public final class RiskRuleEvaluationResult {

    /**
     * RiskRuleEvaluationResult 的 matched 字段。
     */
    private final boolean matched;


    /**
     * RiskRuleEvaluationResult 的 evidence 字段。
     */
    private final List<RiskRuleEvidence> evidence;


    /**
     * RiskRuleEvaluationResult 的 missingFeatures 字段。
     */
    private final List<String> missingFeatures;


    /**
     * 创建 RiskRuleEvaluationResult。
     *
     * @param matched RiskRuleEvaluationResult 的 matched 字段
     * @param evidence RiskRuleEvaluationResult 的 evidence 字段
     * @param missingFeatures RiskRuleEvaluationResult 的 missingFeatures 字段
     */
    public RiskRuleEvaluationResult(boolean matched, List<RiskRuleEvidence> evidence, List<String> missingFeatures) {
        this.matched = matched;
        this.evidence = evidence;
        this.missingFeatures = missingFeatures;
    }

    /**
     * 返回 RiskRuleEvaluationResult 的 matched 字段。
     *
     * @return matched 字段值
     */
    public boolean matched() {
        return matched;
    }

    /**
     * 返回 RiskRuleEvaluationResult 的 evidence 字段。
     *
     * @return evidence 字段值
     */
    public List<RiskRuleEvidence> evidence() {
        return evidence;
    }

    /**
     * 返回 RiskRuleEvaluationResult 的 missingFeatures 字段。
     *
     * @return missingFeatures 字段值
     */
    public List<String> missingFeatures() {
        return missingFeatures;
    }

    /**
     * 比较当前 RiskRuleEvaluationResult 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskRuleEvaluationResult other)) {
            return false;
        }
        return matched == other.matched
                && Objects.equals(evidence, other.evidence)
                && Objects.equals(missingFeatures, other.missingFeatures);
    }

    /**
     * 计算 RiskRuleEvaluationResult 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(matched, evidence, missingFeatures);
    }

    /**
     * 返回 RiskRuleEvaluationResult 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskRuleEvaluationResult[matched=" + matched + ", evidence=" + evidence + ", missingFeatures=" + missingFeatures + "]";
    }
}
