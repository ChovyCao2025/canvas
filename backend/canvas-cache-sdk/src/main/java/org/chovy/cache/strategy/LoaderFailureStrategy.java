package org.chovy.cache.strategy;

/**
 * 数据加载失败处理策略枚举。
 *
 * <p>用于描述 L3 加载器异常时的返回、兜底和异常传播方式。
 * <p>缓存调用方可以据此区分真实未命中和加载链路故障。
 */
public enum LoaderFailureStrategy {
    /**
     * 加载失败时向调用方抛出异常。
     */
    THROW,
    /**
     * 加载失败时返回本地保存的旧值。
     */
    RETURN_STALE,
    /**
     * 加载失败时返回空结果。
     */
    RETURN_EMPTY
}
