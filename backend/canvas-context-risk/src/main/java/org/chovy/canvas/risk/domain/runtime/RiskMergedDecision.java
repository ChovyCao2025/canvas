package org.chovy.canvas.risk.domain.runtime;

import java.util.List;
import java.util.Objects;

/**
 * 合并后的风控决策。
 *
 * @param action 最终动作
 * @param score 最终分数
 * @param riskBand 风险带
 * @param reasons 原因列表
 * @param labels 标签列表
 * @param effectiveSignals 参与强制结果的有效信号
 * @param shadowSignals 仅用于观察的影子信号
 */
public final class RiskMergedDecision {

    /**
     * RiskMergedDecision 的 action 字段。
     */
    private final RiskDecisionAction action;


    /**
     * RiskMergedDecision 的 score 字段。
     */
    private final int score;


    /**
     * RiskMergedDecision 的 riskBand 字段。
     */
    private final RiskBand riskBand;


    /**
     * RiskMergedDecision 的 reasons 字段。
     */
    private final List<String> reasons;


    /**
     * RiskMergedDecision 的 labels 字段。
     */
    private final List<String> labels;


    /**
     * RiskMergedDecision 的 effectiveSignals 字段。
     */
    private final List<RiskDecisionSignal> effectiveSignals;


    /**
     * RiskMergedDecision 的 shadowSignals 字段。
     */
    private final List<RiskDecisionSignal> shadowSignals;


    /**
     * 创建 RiskMergedDecision。
     *
     * @param action RiskMergedDecision 的 action 字段
     * @param score RiskMergedDecision 的 score 字段
     * @param riskBand RiskMergedDecision 的 riskBand 字段
     * @param reasons RiskMergedDecision 的 reasons 字段
     * @param labels RiskMergedDecision 的 labels 字段
     * @param effectiveSignals RiskMergedDecision 的 effectiveSignals 字段
     * @param shadowSignals RiskMergedDecision 的 shadowSignals 字段
     */
    public RiskMergedDecision(RiskDecisionAction action, int score, RiskBand riskBand, List<String> reasons, List<String> labels, List<RiskDecisionSignal> effectiveSignals, List<RiskDecisionSignal> shadowSignals) {
        this.action = action;
        this.score = score;
        this.riskBand = riskBand;
        this.reasons = reasons;
        this.labels = labels;
        this.effectiveSignals = effectiveSignals;
        this.shadowSignals = shadowSignals;
    }

    /**
     * 返回 RiskMergedDecision 的 action 字段。
     *
     * @return action 字段值
     */
    public RiskDecisionAction action() {
        return action;
    }

    /**
     * 返回 RiskMergedDecision 的 score 字段。
     *
     * @return score 字段值
     */
    public int score() {
        return score;
    }

    /**
     * 返回 RiskMergedDecision 的 riskBand 字段。
     *
     * @return riskBand 字段值
     */
    public RiskBand riskBand() {
        return riskBand;
    }

    /**
     * 返回 RiskMergedDecision 的 reasons 字段。
     *
     * @return reasons 字段值
     */
    public List<String> reasons() {
        return reasons;
    }

    /**
     * 返回 RiskMergedDecision 的 labels 字段。
     *
     * @return labels 字段值
     */
    public List<String> labels() {
        return labels;
    }

    /**
     * 返回 RiskMergedDecision 的 effectiveSignals 字段。
     *
     * @return effectiveSignals 字段值
     */
    public List<RiskDecisionSignal> effectiveSignals() {
        return effectiveSignals;
    }

    /**
     * 返回 RiskMergedDecision 的 shadowSignals 字段。
     *
     * @return shadowSignals 字段值
     */
    public List<RiskDecisionSignal> shadowSignals() {
        return shadowSignals;
    }

    /**
     * 比较当前 RiskMergedDecision 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskMergedDecision other)) {
            return false;
        }
        return Objects.equals(action, other.action)
                && score == other.score
                && Objects.equals(riskBand, other.riskBand)
                && Objects.equals(reasons, other.reasons)
                && Objects.equals(labels, other.labels)
                && Objects.equals(effectiveSignals, other.effectiveSignals)
                && Objects.equals(shadowSignals, other.shadowSignals);
    }

    /**
     * 计算 RiskMergedDecision 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(action, score, riskBand, reasons, labels, effectiveSignals, shadowSignals);
    }

    /**
     * 返回 RiskMergedDecision 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskMergedDecision[action=" + action + ", score=" + score + ", riskBand=" + riskBand + ", reasons=" + reasons + ", labels=" + labels + ", effectiveSignals=" + effectiveSignals + ", shadowSignals=" + shadowSignals + "]";
    }
}
