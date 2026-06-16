package org.chovy.canvas.risk.domain.runtime;

import java.util.Objects;

/**
 * 风控策略编译限制。
 *
 * @param maxGroups 最大规则组数
 * @param maxRules 最大规则数
 * @param maxRequiredFeatures 最大依赖特征数
 * @param maxSafeExpressions 最大安全表达式数
 * @param maxCompiledExpressionBytes 最大编译表达式字节数
 */
public final class RiskStrategyCompileLimits {

    /**
     * RiskStrategyCompileLimits 的 maxGroups 字段。
     */
    private final int maxGroups;


    /**
     * RiskStrategyCompileLimits 的 maxRules 字段。
     */
    private final int maxRules;


    /**
     * RiskStrategyCompileLimits 的 maxRequiredFeatures 字段。
     */
    private final int maxRequiredFeatures;


    /**
     * RiskStrategyCompileLimits 的 maxSafeExpressions 字段。
     */
    private final int maxSafeExpressions;


    /**
     * RiskStrategyCompileLimits 的 maxCompiledExpressionBytes 字段。
     */
    private final int maxCompiledExpressionBytes;


    /**
     * 创建 RiskStrategyCompileLimits。
     *
     * @param maxGroups RiskStrategyCompileLimits 的 maxGroups 字段
     * @param maxRules RiskStrategyCompileLimits 的 maxRules 字段
     * @param maxRequiredFeatures RiskStrategyCompileLimits 的 maxRequiredFeatures 字段
     * @param maxSafeExpressions RiskStrategyCompileLimits 的 maxSafeExpressions 字段
     * @param maxCompiledExpressionBytes RiskStrategyCompileLimits 的 maxCompiledExpressionBytes 字段
     */
    public RiskStrategyCompileLimits(int maxGroups, int maxRules, int maxRequiredFeatures, int maxSafeExpressions, int maxCompiledExpressionBytes) {
        this.maxGroups = maxGroups;
        this.maxRules = maxRules;
        this.maxRequiredFeatures = maxRequiredFeatures;
        this.maxSafeExpressions = maxSafeExpressions;
        this.maxCompiledExpressionBytes = maxCompiledExpressionBytes;
    }

    /**
     * 返回 RiskStrategyCompileLimits 的 maxGroups 字段。
     *
     * @return maxGroups 字段值
     */
    public int maxGroups() {
        return maxGroups;
    }

    /**
     * 返回 RiskStrategyCompileLimits 的 maxRules 字段。
     *
     * @return maxRules 字段值
     */
    public int maxRules() {
        return maxRules;
    }

    /**
     * 返回 RiskStrategyCompileLimits 的 maxRequiredFeatures 字段。
     *
     * @return maxRequiredFeatures 字段值
     */
    public int maxRequiredFeatures() {
        return maxRequiredFeatures;
    }

    /**
     * 返回 RiskStrategyCompileLimits 的 maxSafeExpressions 字段。
     *
     * @return maxSafeExpressions 字段值
     */
    public int maxSafeExpressions() {
        return maxSafeExpressions;
    }

    /**
     * 返回 RiskStrategyCompileLimits 的 maxCompiledExpressionBytes 字段。
     *
     * @return maxCompiledExpressionBytes 字段值
     */
    public int maxCompiledExpressionBytes() {
        return maxCompiledExpressionBytes;
    }

    /**
     * 比较当前 RiskStrategyCompileLimits 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskStrategyCompileLimits other)) {
            return false;
        }
        return maxGroups == other.maxGroups
                && maxRules == other.maxRules
                && maxRequiredFeatures == other.maxRequiredFeatures
                && maxSafeExpressions == other.maxSafeExpressions
                && maxCompiledExpressionBytes == other.maxCompiledExpressionBytes;
    }

    /**
     * 计算 RiskStrategyCompileLimits 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(maxGroups, maxRules, maxRequiredFeatures, maxSafeExpressions, maxCompiledExpressionBytes);
    }

    /**
     * 返回 RiskStrategyCompileLimits 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskStrategyCompileLimits[maxGroups=" + maxGroups + ", maxRules=" + maxRules + ", maxRequiredFeatures=" + maxRequiredFeatures + ", maxSafeExpressions=" + maxSafeExpressions + ", maxCompiledExpressionBytes=" + maxCompiledExpressionBytes + "]";
    }

    /**
         * 返回默认编译限制。
         */
        public static RiskStrategyCompileLimits defaults() {
            return new RiskStrategyCompileLimits(50, 500, 200, 0, 0);
        }
}
