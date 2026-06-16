package org.chovy.canvas.risk.domain.runtime;

import java.util.List;
import java.util.Objects;

/**
 * 风控决策合并请求，携带候选信号、缺失运行时依赖和失败策略。
 *
 * @param signals 候选决策信号
 * @param missingFeatures 缺失的运行时特征或依赖
 * @param failPolicy 依赖失败时的兜底策略
 */
public final class RiskDecisionMergeRequest {

    /**
     * RiskDecisionMergeRequest 的 signals 字段。
     */
    private final List<RiskDecisionSignal> signals;


    /**
     * RiskDecisionMergeRequest 的 missingFeatures 字段。
     */
    private final List<String> missingFeatures;


    /**
     * RiskDecisionMergeRequest 的 failPolicy 字段。
     */
    private final RiskFailPolicy failPolicy;


    /**
     * 创建 RiskDecisionMergeRequest。
     *
     * @param signals RiskDecisionMergeRequest 的 signals 字段
     * @param missingFeatures RiskDecisionMergeRequest 的 missingFeatures 字段
     * @param failPolicy RiskDecisionMergeRequest 的 failPolicy 字段
     */
    public RiskDecisionMergeRequest(List<RiskDecisionSignal> signals, List<String> missingFeatures, RiskFailPolicy failPolicy) {
        this.signals = signals;
        this.missingFeatures = missingFeatures;
        this.failPolicy = failPolicy;
    }

    /**
     * 返回 RiskDecisionMergeRequest 的 signals 字段。
     *
     * @return signals 字段值
     */
    public List<RiskDecisionSignal> signals() {
        return signals;
    }

    /**
     * 返回 RiskDecisionMergeRequest 的 missingFeatures 字段。
     *
     * @return missingFeatures 字段值
     */
    public List<String> missingFeatures() {
        return missingFeatures;
    }

    /**
     * 返回 RiskDecisionMergeRequest 的 failPolicy 字段。
     *
     * @return failPolicy 字段值
     */
    public RiskFailPolicy failPolicy() {
        return failPolicy;
    }

    /**
     * 比较当前 RiskDecisionMergeRequest 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskDecisionMergeRequest other)) {
            return false;
        }
        return Objects.equals(signals, other.signals)
                && Objects.equals(missingFeatures, other.missingFeatures)
                && Objects.equals(failPolicy, other.failPolicy);
    }

    /**
     * 计算 RiskDecisionMergeRequest 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(signals, missingFeatures, failPolicy);
    }

    /**
     * 返回 RiskDecisionMergeRequest 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskDecisionMergeRequest[signals=" + signals + ", missingFeatures=" + missingFeatures + ", failPolicy=" + failPolicy + "]";
    }

    /**
         * 创建强制执行模式下的合并请求，默认按人工复核兜底。
         */
        public static RiskDecisionMergeRequest enforce(List<RiskDecisionSignal> signals) {
            return new RiskDecisionMergeRequest(
                    signals == null ? List.of() : List.copyOf(signals),
                    List.of(),
                    RiskFailPolicy.FAIL_REVIEW);
        }

        /**
         * 返回替换缺失特征列表后的请求副本。
         */
        public RiskDecisionMergeRequest withMissingFeatures(List<String> features) {
            return new RiskDecisionMergeRequest(signals,
                    features == null ? List.of() : List.copyOf(features),
                    failPolicy);
        }

        /**
         * 返回替换失败策略后的请求副本。
         */
        public RiskDecisionMergeRequest withFailPolicy(RiskFailPolicy policy) {
            return new RiskDecisionMergeRequest(signals, missingFeatures,
                    policy == null ? RiskFailPolicy.FAIL_REVIEW : policy);
        }
}
