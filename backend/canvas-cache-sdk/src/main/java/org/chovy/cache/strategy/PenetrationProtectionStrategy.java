package org.chovy.cache.strategy;

/**
 * 缓存穿透保护策略枚举。
 *
 * <p>用于描述不存在 key 的拦截方式，例如空值缓存或布隆过滤。
 * <p>该策略减少无效请求持续穿透到数据库或下游服务。
 */
public enum PenetrationProtectionStrategy {
    NONE,
    CACHE_NULL_SHORT_TTL,
    CACHE_EMPTY_SHORT_TTL,
    BLOOM_FILTER,
    KEY_VALIDATOR,
    CACHE_NULL_AND_BLOOM,
    FULL
}
