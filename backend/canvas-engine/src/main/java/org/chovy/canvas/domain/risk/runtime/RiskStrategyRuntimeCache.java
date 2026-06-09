package org.chovy.canvas.domain.risk.runtime;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 风控策略运行时编译缓存，按策略快照缓存可执行计划。
 */
public class RiskStrategyRuntimeCache {

    private final RiskStrategyCompiler compiler;
    private final ConcurrentMap<String, RiskCompiledStrategy> cache = new ConcurrentHashMap<>();

    /**
     * 创建运行时缓存。
     */
    public RiskStrategyRuntimeCache(RiskStrategyCompiler compiler) {
        this.compiler = compiler;
    }

    /**
     * 获取已编译策略，缓存未命中时立即编译并保存。
     */
    public RiskCompiledStrategy getOrCompile(RiskStrategySnapshot snapshot) {
        return cache.computeIfAbsent(snapshot.cacheKey(), ignored -> compiler.compile(snapshot));
    }

    /**
     * 清理指定策略版本的编译缓存。
     */
    public void invalidate(Long tenantId, String sceneKey, String strategyKey, int version) {
        cache.remove(tenantId + ":" + sceneKey + ":" + strategyKey + ":" + version);
    }
}
