package org.chovy.cache.strategy;

/**
 * Redis 故障处理策略枚举。
 *
 * <p>用于描述 L2 缓存不可用时是降级到 L1/L3、快速失败还是忽略写入失败。
 * <p>策略化处理可以避免缓存故障直接扩大为业务故障。
 */
public enum RedisFailureStrategy {
    /**
     * Redis 故障时降级到后续缓存或加载链路。
     */
    FALLTHROUGH,
    /**
     * Redis 故障时立即向调用方抛出异常。
     */
    FAIL_FAST
}
