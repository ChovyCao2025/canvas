package org.chovy.canvas.domain.risk.runtime;

import java.util.List;

/**
 * 风控规则评估结果。
 *
 * @param matched 规则是否匹配
 * @param evidence 条件级评估证据
 * @param missingFeatures 评估过程中缺失的特征
 */
public record RiskRuleEvaluationResult(
        boolean matched,
        List<RiskRuleEvidence> evidence,
        List<String> missingFeatures
) {
}
