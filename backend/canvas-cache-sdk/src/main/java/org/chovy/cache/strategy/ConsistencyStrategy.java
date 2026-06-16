package org.chovy.cache.strategy;

/**
 * 缓存一致性策略枚举。
 *
 * <p>用于描述写路径上缓存失效、延迟双删或主动刷新等行为。
 * <p>该枚举帮助缓存实例在性能和一致性之间选择明确取舍。
 */
public enum ConsistencyStrategy {
    /**
     * 写入后允许缓存最终一致。
     */
    EVENTUAL,
    /**
     * 写入后执行延迟双删降低脏读概率。
     */
    DOUBLE_DELETE
}
