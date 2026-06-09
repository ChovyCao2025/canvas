package org.chovy.canvas.web.risk;

/**
 * Web 层策略运行时缓存失效接口。
 */
public interface RiskStrategyRuntimeCache {

    /**
     * 清理指定租户策略的运行时缓存。
     */
    void invalidate(Long tenantId, String strategyKey);
}
