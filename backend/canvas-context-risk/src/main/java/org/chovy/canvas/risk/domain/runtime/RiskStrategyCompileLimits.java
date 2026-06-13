package org.chovy.canvas.risk.domain.runtime;

/**
 * 风控策略编译限制。
 *
 * @param maxGroups 最大规则组数
 * @param maxRules 最大规则数
 * @param maxRequiredFeatures 最大依赖特征数
 * @param maxSafeExpressions 最大安全表达式数
 * @param maxCompiledExpressionBytes 最大编译表达式字节数
 */
public record RiskStrategyCompileLimits(
        int maxGroups,
        int maxRules,
        int maxRequiredFeatures,
        int maxSafeExpressions,
        int maxCompiledExpressionBytes
) {

    /**
     * 返回默认编译限制。
     */
    public static RiskStrategyCompileLimits defaults() {
        return new RiskStrategyCompileLimits(50, 500, 200, 0, 0);
    }
}
