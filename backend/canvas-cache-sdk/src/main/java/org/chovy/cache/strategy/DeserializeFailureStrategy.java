package org.chovy.cache.strategy;

/**
 * 缓存反序列化失败处理策略枚举。
 *
 * <p>用于描述 Redis 中缓存值无法反序列化时是删除、跳过还是直接失败。
 * <p>集中策略有助于避免脏缓存导致业务链路持续异常。
 */
public enum DeserializeFailureStrategy {
    FALLTHROUGH_TO_L3,
    THROW
}
