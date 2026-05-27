package org.chovy.cache.testing;

import org.chovy.cache.ReactiveTieredCache;
import org.chovy.cache.TieredCache;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * InMemoryTieredCache 是分层缓存 SDK 的测试支撑组件。
 *
 * <p>用于在业务或单元测试中替代真实 Redis/多级缓存依赖，使缓存调用可以在内存中稳定验证。
 * <p>该组件只服务测试场景，不应承载生产缓存策略。
 */
public class InMemoryTieredCache<K, V> implements TieredCache<K, V> {
    /** 测试缓存实例名称。 */
    private final String name;
    /** 测试未命中时使用的数据加载函数。 */
    private final Function<K, V> loader;
    /** 内存缓存数据存储。 */
    private final ConcurrentHashMap<K, Optional<V>> store = new ConcurrentHashMap<>();
    /** 内存缓存命中次数。 */
    private final LongAdder hits = new LongAdder();
    /** 内存缓存未命中次数。 */
    private final LongAdder misses = new LongAdder();

    /**
     * 构造 InMemoryTieredCache 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param name name 方法执行所需的业务参数
     * @param loader loader 方法执行所需的业务参数
     */
    private InMemoryTieredCache(String name, Function<K, V> loader) {
        this.name = name;
        this.loader = loader;
    }

    /**
     * 执行 of 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param loader loader 方法执行所需的业务参数
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public static <K, V> InMemoryTieredCache<K, V> of(Function<K, V> loader) {
        return new InMemoryTieredCache<>("test", loader);
    }

    /**
     * 执行 of 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param name name 方法执行所需的业务参数
     * @param loader loader 方法执行所需的业务参数
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public static <K, V> InMemoryTieredCache<K, V> of(String name, Function<K, V> loader) {
        return new InMemoryTieredCache<>(name, loader);
    }

    /**
     * 返回 InMemoryTieredCache 的 name 配置值。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * 查询或读取 get 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
    @Override
    public Optional<V> get(K key) {
        Optional<V> existing = store.get(key);
        if (existing != null) {
            hits.increment();
            return existing;
        }
        misses.increment();
        // 测试实现同样缓存 Optional.empty，便于覆盖空值缓存和穿透保护相关用例。
        Optional<V> loaded = Optional.ofNullable(loader.apply(key));
        store.put(key, loaded);
        return loaded;
    }

    /**
     * 查询或读取 get If Present 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
    @Override
    public Optional<V> getIfPresent(K key) {
        Optional<V> existing = store.get(key);
        if (existing != null) {
            hits.increment();
            return existing;
        }
        misses.increment();
        return Optional.empty();
    }

    /**
     * 查询或读取 get 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param loaderOverride loaderOverride 方法执行所需的业务参数
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
    @Override
    public Optional<V> get(K key, Supplier<V> loaderOverride) {
        // computeIfAbsent 模拟单 key 的未命中加载，避免并发测试中重复调用 loaderOverride。
        return store.computeIfAbsent(key, ignored -> Optional.ofNullable(loaderOverride.get()));
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
    @Override
    public Map<K, Optional<V>> getAll(Collection<K> keys, Function<Collection<K>, Map<K, V>> batchLoader) {
        Map<K, Optional<V>> result = new LinkedHashMap<>();
        java.util.List<K> misses = new java.util.ArrayList<>();
        for (K key : keys) {
            Optional<V> existing = store.get(key);
            if (existing == null) {
                misses.add(key);
            } else {
                result.put(key, existing);
            }
        }
        // 只把真正未命中的 key 交给 batchLoader，模拟生产缓存的批量回源边界。
        Map<K, V> loaded = misses.isEmpty() ? Map.of() : batchLoader.apply(misses);
        for (K key : misses) {
            Optional<V> value = Optional.ofNullable(loaded.get(key));
            store.put(key, value);
            result.put(key, value);
        }
        return result;
    }

    /**
     * 写入或记录 put 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param value value 待写入、比较或转换的业务值
     */
    @Override
    public void put(K key, V value) {
        store.put(key, Optional.ofNullable(value));
    }

    /**
     * 删除、清理或失效 invalidate 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     */
    @Override
    public void invalidate(K key) {
        store.remove(key);
    }

    /**
     * 执行 safe Write 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param writeAction writeAction 方法执行所需的业务参数
     * @param delayMs delayMs 方法执行所需的业务参数
     */
    @Override
    public void safeWrite(K key, Runnable writeAction, long delayMs) {
        // 测试实现同步模拟延迟双删的顺序，不实际等待 delayMs，避免单测变慢。
        invalidate(key);
        writeAction.run();
        invalidate(key);
    }

    /**
     * 更新或刷新 refresh 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     */
    @Override
    public void refresh(K key) {
        invalidate(key);
        get(key);
    }

    /**
     * 执行 as Reactive 对应的业务逻辑。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @return 方法执行后的业务结果
     */
    @Override
    public ReactiveTieredCache<K, V> asReactive() {
        return new ReactiveTieredCache<>() {
            @Override
            public Mono<Optional<V>> get(K key) {
                // 通过 fromCallable 保持响应式接口形态，同时复用同步测试缓存的状态和计数。
                return Mono.fromCallable(() -> InMemoryTieredCache.this.get(key));
            }

            @Override
            public Mono<Optional<V>> getIfPresent(K key) {
                return Mono.fromCallable(() -> InMemoryTieredCache.this.getIfPresent(key));
            }

            @Override
            public Mono<Void> put(K key, V value) {
                return Mono.fromRunnable(() -> InMemoryTieredCache.this.put(key, value)).then();
            }

            @Override
            public Mono<Void> invalidate(K key) {
                return Mono.fromRunnable(() -> InMemoryTieredCache.this.invalidate(key)).then();
            }

            @Override
            public Mono<Void> refresh(K key) {
                return Mono.fromRunnable(() -> InMemoryTieredCache.this.refresh(key)).then();
            }
        };
    }

    @Override
    public org.chovy.cache.TieredCacheStats stats() {
        return new org.chovy.cache.TieredCacheStats(name, store.size(), hits.sum(), misses.sum(), 0, 0, 0, 0, 0);
    }
}
