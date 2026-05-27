package org.chovy.cache.strategy;

/**
 * 缓存雪崩保护策略枚举。
 *
 * <p>用于描述热点缓存同时过期时的保护方式，例如 TTL 抖动或提前刷新。
 * <p>缓存实现根据该枚举选择具体行为，业务侧只需要声明策略意图。
 */
public enum AvalancheProtectionStrategy {
    NONE,
    TTL_JITTER,
    REFRESH_AHEAD,
    STALE_ON_ERROR,
    FULL
}
