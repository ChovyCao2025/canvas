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
     * 返回 TieredCache 的 name 配置值。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    String name();

    /**
     * 查询或读取 get 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
    Optional<V> get(K key);

    /**
     * 查询或读取 get If Present 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
    Optional<V> getIfPresent(K key);

    /**
     * 查询或读取 get 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param loaderOverride loaderOverride 方法执行所需的业务参数
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
    Optional<V> get(K key, Supplier<V> loaderOverride);

    /**
     * 查询或读取 get All 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param keys keys 对应的缓存键、配置键或业务键
     * @return 可能存在的查询结果，未命中或无数据时为空
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
     * 查询或读取 get All 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param keys keys 对应的缓存键、配置键或业务键
     * @param batchLoader batchLoader 方法执行所需的业务参数
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
    Map<K, Optional<V>> getAll(Collection<K> keys, Function<Collection<K>, Map<K, V>> batchLoader);

    /**
     * 查询或读取 get Or Throw 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 方法执行后的业务结果
     */
    default V getOrThrow(K key) {
        return get(key).orElseThrow(() ->
                new NoSuchElementException("Cache miss: " + name() + " key=" + key));
    }

    /**
     * 写入或记录 put 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param value value 待写入、比较或转换的业务值
     */
    void put(K key, V value);

    /**
     * 写入或记录 put All 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param values values 待写入、比较或转换的业务值
     */
    default void putAll(Map<K, V> values) {
        values.forEach(this::put);
    }

    /**
     * 删除、清理或失效 invalidate 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     */
    void invalidate(K key);

    /**
     * 删除、清理或失效 invalidate All 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param keys keys 对应的缓存键、配置键或业务键
     */
    default void invalidateAll(Collection<K> keys) {
        // 批量失效逐 key 触发，确保实现类的本地清理、Redis 清理和广播逻辑都被执行。
        keys.forEach(this::invalidate);
    }

    /**
     * 执行 warmup 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param keys keys 对应的缓存键、配置键或业务键
     */
    default void warmup(Collection<K> keys) {
        // 预热通过普通 get 进入完整加载和回填链路，避免绕过穿透/击穿保护策略。
        keys.forEach(this::get);
    }

    /**
     * 执行 stats 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    TieredCacheStats stats();

    /**
     * 执行 safe Write 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param writeAction writeAction 方法执行所需的业务参数
     * @param delayMs delayMs 方法执行所需的业务参数
     */
    void safeWrite(K key, Runnable writeAction, long delayMs);

    /**
     * 更新或刷新 refresh 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     */
    void refresh(K key);

    /**
     * 执行 as Reactive 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    ReactiveTieredCache<K, V> asReactive();
}
