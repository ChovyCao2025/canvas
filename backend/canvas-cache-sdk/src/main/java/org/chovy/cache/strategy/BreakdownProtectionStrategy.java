package org.chovy.cache.strategy;

/**
 * 缓存击穿保护策略枚举。
 *
 * <p>用于描述热点 key 失效后如何避免大量请求同时打到加载器或数据库。
 * <p>常见实现包括互斥加载、逻辑过期和单飞合并。
 */
public enum BreakdownProtectionStrategy {
    NONE,
    LOCAL_SINGLE_FLIGHT,
    DISTRIBUTED_LOCK,
    STALE_WHILE_REVALIDATE,
    LOCAL_AND_DISTRIBUTED,
    FULL
}
