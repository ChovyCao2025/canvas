package org.chovy.canvas.risk.domain.runtime;

import java.util.List;

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
public record RiskMergedDecision(
        RiskDecisionAction action,
        int score,
        RiskBand riskBand,
        List<String> reasons,
        List<String> labels,
        List<RiskDecisionSignal> effectiveSignals,
        List<RiskDecisionSignal> shadowSignals
) {
}
