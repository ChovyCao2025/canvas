package org.chovy.cache.strategy;

/**
 * 缓存击穿保护策略枚举。
 *
 * <p>用于描述热点 key 失效后如何避免大量请求同时打到加载器或数据库。
 * <p>常见实现包括互斥加载、逻辑过期和单飞合并。
 */
public enum BreakdownProtectionStrategy {
    /**
     * 不启用缓存击穿保护。
     */
    NONE,
    /**
     * 使用本地单飞合并同一 key 的并发加载。
     */
    LOCAL_SINGLE_FLIGHT,
    /**
     * 使用 Redis 分布式锁串行化同一 key 的加载。
     */
    DISTRIBUTED_LOCK,
    /**
     * 旧值可用时先返回旧值并异步刷新。
     */
    STALE_WHILE_REVALIDATE,
    /**
     * 组合本地单飞与分布式锁保护。
     */
    LOCAL_AND_DISTRIBUTED,
    /**
     * 启用全部击穿保护手段。
     */
    FULL
}
