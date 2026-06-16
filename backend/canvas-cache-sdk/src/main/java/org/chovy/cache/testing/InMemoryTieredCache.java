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
    /**
     * 测试缓存实例名称。
     */
    private final String name;

    /**
     * 测试未命中时使用的数据加载函数。
     */
    private final Function<K, V> loader;

    /**
     * 内存缓存数据存储。
     */
    private final ConcurrentHashMap<K, Optional<V>> store = new ConcurrentHashMap<>();

    /**
     * 内存缓存命中次数。
     */
    private final LongAdder hits = new LongAdder();

    /**
     * 内存缓存未命中次数。
     */
    private final LongAdder misses = new LongAdder();

    /**
     * 创建内存版测试缓存。
     *
     * <p>该构造器只在工厂方法内使用，保证测试缓存带有名称和未命中加载函数。
     *
     * @param name 测试缓存名称
     * @param loader 未命中时使用的测试数据加载函数
     */
    private InMemoryTieredCache(String name, Function<K, V> loader) {
        this.name = name;
        this.loader = loader;
    }

    /**
     * 创建默认名称的内存测试缓存。
     *
     * <p>默认名称为 {@code test}，适合不关心 cacheName 的单元测试。
     *
     * @param loader 未命中加载函数
     * @return 内存测试缓存实例
     */
    public static <K, V> InMemoryTieredCache<K, V> of(Function<K, V> loader) {
        return new InMemoryTieredCache<>("test", loader);
    }

    /**
     * 创建指定名称的内存测试缓存。
     *
     * <p>指定名称便于测试注解切面或多缓存场景中按 cacheName 区分实例。
     *
     * @param name 测试缓存名称
     * @param loader 未命中加载函数
     * @return 内存测试缓存实例
     */
    public static <K, V> InMemoryTieredCache<K, V> of(String name, Function<K, V> loader) {
        return new InMemoryTieredCache<>(name, loader);
    }

    /**
     * 返回测试缓存名称。
     *
     * <p>名称用于模拟生产缓存的注册、查找和指标标识。
     *
     * @return 测试缓存名称
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * 从内存 Map 读取缓存，未命中时调用测试 loader。
     *
     * <p>该实现同样缓存 Optional.empty，用于覆盖空值缓存和穿透保护相关测试。
     *
     * @param key 业务缓存 key
     * @return 缓存值或空值占位
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
     * 只读取内存中已有的缓存值。
     *
     * <p>未命中时不会调用 loader，便于测试只读缓存探测逻辑。
     *
     * @param key 业务缓存 key
     * @return 已缓存的值或空结果
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
     * 使用本次调用提供的 loader 读取缓存。
     *
     * <p>内存实现通过 computeIfAbsent 模拟单 key 未命中加载，方便并发测试验证 loader 只执行一次。
     *
     * @param key 业务缓存 key
     * @param loaderOverride 本次未命中加载函数
     * @return 缓存值或空值占位
     */
    @Override
    public Optional<V> get(K key, Supplier<V> loaderOverride) {
        // computeIfAbsent 模拟单 key 的未命中加载，避免并发测试中重复调用 loaderOverride。
        return store.computeIfAbsent(key, ignored -> Optional.ofNullable(loaderOverride.get()));
    }

    /**
     * 批量读取内存缓存并对未命中 key 批量加载。
     *
     * <p>只有真实未命中的 key 会传给 batchLoader，用于模拟生产批量回源边界。
     *
     * @param keys 需要读取的业务缓存 key 集合
     * @param batchLoader 未命中 key 的批量加载函数
     * @return 每个 key 对应的缓存值
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
        if (loaded == null) {
            loaded = Map.of();
        }
        for (K key : misses) {
            Optional<V> value = Optional.ofNullable(loaded.get(key));
            store.put(key, value);
            result.put(key, value);
        }
        return result;
    }

    /**
     * 写入内存缓存。
     *
     * <p>null 值会被保存为 Optional.empty，以便测试空值缓存语义。
     *
     * @param key 业务缓存 key
     * @param value 待缓存值
     */
    @Override
    public void put(K key, V value) {
        store.put(key, Optional.ofNullable(value));
    }

    /**
     * 从内存缓存中移除指定 key。
     *
     * <p>测试实现不发布跨节点失效事件，只影响当前内存实例。
     *
     * @param key 需要失效的业务缓存 key
     */
    @Override
    public void invalidate(K key) {
        store.remove(key);
    }

    /**
     * 同步模拟延迟双删写路径。
     *
     * <p>测试实现会按“删缓存、执行业务写入、再删缓存”的顺序执行，但不会真实等待 delayMs。
     *
     * @param key 需要保护一致性的业务缓存 key
     * @param writeAction 测试中的写入动作
     * @param delayMs 生产语义中的延迟毫秒数，测试实现不等待
     */
    @Override
    public void safeWrite(K key, Runnable writeAction, long delayMs) {
        // 测试实现同步模拟延迟双删的顺序，不实际等待 delayMs，避免单测变慢。
        invalidate(key);
        writeAction.run();
        invalidate(key);
    }

    /**
     * 刷新内存缓存中的指定 key。
     *
     * <p>通过先失效再调用 get，模拟生产缓存重新回源并写回的行为。
     *
     * @param key 需要刷新的业务缓存 key
     */
    @Override
    public void refresh(K key) {
        invalidate(key);
        get(key);
    }

    /**
     * 返回内存测试缓存的响应式视图。
     *
     * <p>响应式方法用 Mono 包装同步内存实现，共享同一份 store 和命中统计。
     *
     * @return 响应式测试缓存接口
     */
    @Override
    public ReactiveTieredCache<K, V> asReactive() {
        return new ReactiveTieredCache<>() {
            /**
             * 响应式读取缓存，订阅时复用同步 get。
             *
             * @param key 业务缓存 key
             * @return 订阅后产生的缓存读取结果
             */
            @Override
            public Mono<Optional<V>> get(K key) {
                // 通过 fromCallable 保持响应式接口形态，同时复用同步测试缓存的状态和计数。
                return Mono.fromCallable(() -> InMemoryTieredCache.this.get(key));
            }

            /**
             * 响应式读取已存在缓存，订阅时复用同步 getIfPresent。
             *
             * @param key 业务缓存 key
             * @return 订阅后产生的已缓存值
             */
            @Override
            public Mono<Optional<V>> getIfPresent(K key) {
                return Mono.fromCallable(() -> InMemoryTieredCache.this.getIfPresent(key));
            }

            /**
             * 响应式写入测试缓存。
             *
             * @param key 业务缓存 key
             * @param value 待缓存值
             * @return 写入完成信号
             */
            @Override
            public Mono<Void> put(K key, V value) {
                return Mono.fromRunnable(() -> InMemoryTieredCache.this.put(key, value)).then();
            }

            /**
             * 响应式失效测试缓存 key。
             *
             * @param key 需要失效的业务缓存 key
             * @return 失效完成信号
             */
            @Override
            public Mono<Void> invalidate(K key) {
                return Mono.fromRunnable(() -> InMemoryTieredCache.this.invalidate(key)).then();
            }

            /**
             * 响应式刷新测试缓存 key。
             *
             * @param key 需要刷新的业务缓存 key
             * @return 刷新完成信号
             */
            @Override
            public Mono<Void> refresh(K key) {
                return Mono.fromRunnable(() -> InMemoryTieredCache.this.refresh(key)).then();
            }
        };
    }

    /**
     * 返回内存测试缓存的统计快照。
     *
     * <p>只统计当前 JVM 内的 store 大小、命中和未命中次数，L2/L3 相关计数固定为 0。
     *
     * @return 测试缓存统计快照
     */
    @Override
    public org.chovy.cache.TieredCacheStats stats() {
        return new org.chovy.cache.TieredCacheStats(name, store.size(), hits.sum(), misses.sum(), 0, 0, 0, 0, 0);
    }
}
