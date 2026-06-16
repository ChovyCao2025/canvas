package org.chovy.cache.strategy;

/**
 * 缓存雪崩保护策略枚举。
 *
 * <p>用于描述热点缓存同时过期时的保护方式，例如 TTL 抖动或提前刷新。
 * <p>缓存实现根据该枚举选择具体行为，业务侧只需要声明策略意图。
 */
public enum AvalancheProtectionStrategy {
    /**
     * 不启用缓存雪崩保护。
     */
    NONE,
    /**
     * 为过期时间增加随机抖动。
     */
    TTL_JITTER,
    /**
     * 在过期前提前刷新热点缓存。
     */
    REFRESH_AHEAD,
    /**
     * 异常时优先返回旧值兜底。
     */
    STALE_ON_ERROR,
    /**
     * 启用全部雪崩保护手段。
     */
    FULL
}
