package org.chovy.cache;

import java.util.NoSuchElementException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 分层缓存核心接口，统一封装 L1 本地缓存、L2 Redis 缓存与 L3 数据加载器的访问语义。
 *
 * <p>调用方通过 get/put/invalidate 等方法使用缓存，不需要感知各层命中、回填、预热和延迟双删的内部细节。
 * <p>默认方法提供批量读取、强制命中、批量写入和预热的便捷实现，具体一致性策略由实现类处理。
 */
public interface TieredCache<K, V> {
    /**
     * 返回缓存实例名称。
     *
     * <p>该名称用于注册中心查找、指标标签、Redis 失效频道和注解切面解析，要求在同一应用内唯一。
     *
     * @return 缓存实例名称
     */
    String name();

    /**
     * 按完整分层缓存链路读取单个 key。
     *
     * <p>实现通常先查 L1 本地缓存，再查 L2 Redis，最后回源到 L3 加载器；命中低层后会按策略回填上层。
     * <p>返回 {@link Optional#empty()} 表示业务值为空或加载器返回 null，具体是否缓存空值由穿透保护策略决定。
     *
     * @param key 业务缓存 key
     * @return 缓存值；业务不存在或空值占位命中时为空
     */
    Optional<V> get(K key);

    /**
     * 只读取当前缓存中已存在的数据，不触发 L3 回源加载。
     *
     * <p>该方法适合旁路探测和短路判断；实现可以查 L1/L2，但不应调用默认加载器或覆盖当前缓存内容。
     *
     * @param key 业务缓存 key
     * @return 已缓存的值；缓存未命中或空值占位命中时为空
     */
    Optional<V> getIfPresent(K key);

    /**
     * 使用本次调用提供的加载器读取单个 key。
     *
     * <p>缓存命中时不会执行 {@code loaderOverride}；未命中时该加载器替代构建缓存时配置的默认 L3 加载器。
     * <p>击穿、穿透、Redis 失败和空值缓存策略仍由缓存实例统一控制。
     *
     * @param key 业务缓存 key
     * @param loaderOverride 本次未命中时使用的数据加载函数
     * @return 缓存值；业务不存在或空值占位命中时为空
     */
    Optional<V> get(K key, Supplier<V> loaderOverride);

    /**
     * 批量读取多个 key，并复用单 key 的完整分层读取语义。
     *
     * <p>默认实现逐个调用 {@link #get(Object)}，因此会触发各 key 的 L1/L2/L3 链路和回填逻辑。
     *
     * @param keys 需要读取的业务缓存 key 集合
     * @return 按输入遍历顺序写入的 key 到缓存值映射
     */
    default Map<K, Optional<V>> getAll(Collection<K> keys) {
        Map<K, Optional<V>> result = new LinkedHashMap<>();
        for (K key : keys) {
            // 默认批量读取复用单 key 读取语义，具体实现仍可覆写为批量加载。
            result.put(key, get(key));
        }
        return result;
    }

    /**
     * 批量读取多个 key，并允许实现类对未命中的 key 批量回源。
     *
     * <p>缓存实现应只把真正未命中的 key 交给 {@code batchLoader}，并将加载结果按空值和 Redis 写入策略回填。
     *
     * @param keys 需要读取的业务缓存 key 集合
     * @param batchLoader 未命中 key 的批量数据加载函数
     * @return 每个 key 对应的缓存值；加载结果缺失的 key 映射为空
     */
    Map<K, Optional<V>> getAll(Collection<K> keys, Function<Collection<K>, Map<K, V>> batchLoader);

    /**
     * 读取单个 key，并在业务值不存在时抛出异常。
     *
     * <p>该方法适用于调用方把缓存未命中视为错误的场景；仍会经过完整 get 链路。
     *
     * @param key 业务缓存 key
     * @return 非空业务值
     * @throws NoSuchElementException 缓存链路和加载器均未得到业务值时抛出
     */
    default V getOrThrow(K key) {
        return get(key).orElseThrow(() ->
                new NoSuchElementException("Cache miss: " + name() + " key=" + key));
    }

    /**
     * 主动写入单个缓存值。
     *
     * <p>实现应同时更新 L1 和 L2，并按配置发布本地缓存失效事件，保证其他节点不会继续读取旧值。
     *
     * @param key 业务缓存 key
     * @param value 待缓存的业务值
     */
    void put(K key, V value);

    /**
     * 批量写入缓存值。
     *
     * <p>默认逐条调用 {@link #put(Object, Object)}，确保每个 key 都执行一致的 L1/L2 写入和失效广播逻辑。
     *
     * @param values 业务缓存 key 到待写入值的映射
     */
    default void putAll(Map<K, V> values) {
        values.forEach(this::put);
    }

    /**
     * 失效单个缓存 key。
     *
     * <p>实现应清理本节点 L1、删除 L2 Redis 数据，并发布跨节点本地缓存失效事件。
     *
     * @param key 需要失效的业务缓存 key
     */
    void invalidate(K key);

    /**
     * 批量失效缓存 key。
     *
     * <p>默认逐个调用 {@link #invalidate(Object)}，保证每个 key 的 L1/L2 清理和广播语义一致。
     *
     * @param keys 需要失效的业务缓存 key 集合
     */
    default void invalidateAll(Collection<K> keys) {
        // 批量失效逐 key 触发，确保实现类的本地清理、Redis 清理和广播逻辑都被执行。
        keys.forEach(this::invalidate);
    }

    /**
     * 预热一组缓存 key。
     *
     * <p>预热通过普通 get 进入完整加载和回填链路，适合启动后或流量切换前提前填充热点数据。
     *
     * @param keys 需要预热的业务缓存 key 集合
     */
    default void warmup(Collection<K> keys) {
        // 预热通过普通 get 进入完整加载和回填链路，避免绕过穿透/击穿保护策略。
        keys.forEach(this::get);
    }

    /**
     * 获取当前缓存实例的运行统计快照。
     *
     * <p>统计包括 L1/L2 命中未命中、L3 加载、穿透拒绝和加载失败等计数，调用该方法不重置计数器。
     *
     * @return 当前时刻的缓存指标快照
     */
    TieredCacheStats stats();

    /**
     * 执行业务写入并通过延迟双删降低数据库与缓存不一致窗口。
     *
     * <p>典型流程是在写数据库前先失效缓存，执行 {@code writeAction}，再延迟一段时间后再次失效缓存。
     *
     * @param key 需要保护一致性的业务缓存 key
     * @param writeAction 实际写入数据库或下游系统的动作
     * @param delayMs 第二次失效前等待的毫秒数
     */
    void safeWrite(K key, Runnable writeAction, long delayMs);

    /**
     * 强制刷新单个 key 的缓存值。
     *
     * <p>实现通常先失效旧值，再通过加载器重新读取并回填 L1/L2；加载失败时按 loaderFailure 策略处理。
     *
     * @param key 需要刷新的业务缓存 key
     */
    void refresh(K key);

    /**
     * 获取响应式缓存视图。
     *
     * <p>响应式视图应保持与同步接口一致的缓存层级、回填和失败策略，并以 Reactor {@code Mono} 暴露异步结果。
     *
     * @return 当前缓存实例的响应式访问接口
     */
    ReactiveTieredCache<K, V> asReactive();
}
