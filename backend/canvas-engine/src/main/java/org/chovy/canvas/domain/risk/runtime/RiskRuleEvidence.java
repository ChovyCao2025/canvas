package org.chovy.canvas.domain.risk.runtime;

/**
 * 风控规则条件评估证据。
 *
 * @param path 条件路径
 * @param operator 操作符
 * @param leftValue 左值
 * @param rightValue 右值
 * @param matched 条件是否匹配
 */
public record RiskRuleEvidence(
        String path,
        String operator,
        Object leftValue,
        Object rightValue,
        boolean matched
) {
}
