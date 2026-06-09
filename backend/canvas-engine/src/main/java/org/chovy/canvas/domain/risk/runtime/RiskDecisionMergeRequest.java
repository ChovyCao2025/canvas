package org.chovy.canvas.domain.risk.runtime;

import java.util.List;

/**
 * 风控决策合并请求，携带候选信号、缺失运行时依赖和失败策略。
 *
 * @param signals 候选决策信号
 * @param missingFeatures 缺失的运行时特征或依赖
 * @param failPolicy 依赖失败时的兜底策略
 */
public record RiskDecisionMergeRequest(
        List<RiskDecisionSignal> signals,
        List<String> missingFeatures,
        RiskFailPolicy failPolicy
) {

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
