package org.chovy.cache.strategy;

/**
 * 缓存穿透保护策略枚举。
 *
 * <p>用于描述不存在 key 的拦截方式，例如空值缓存或布隆过滤。
 * <p>该策略减少无效请求持续穿透到数据库或下游服务。
 */
public enum PenetrationProtectionStrategy {
    /** 不启用缓存穿透保护。 */
    NONE,
    /** 对不存在的数据短暂缓存空对象占位。 */
    CACHE_NULL_SHORT_TTL,
    /** 对空集合或空结果短暂缓存占位。 */
    CACHE_EMPTY_SHORT_TTL,
    /** 使用布隆过滤器拦截明显不存在的 key。 */
    BLOOM_FILTER,
    /** 使用 key 校验器拦截非法 key。 */
    KEY_VALIDATOR,
    /** 组合空对象短 TTL 与布隆过滤器保护。 */
    CACHE_NULL_AND_BLOOM,
    /** 启用全部穿透保护手段。 */
    FULL
}
