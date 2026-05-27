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
    String name();

    Optional<V> get(K key);

    Optional<V> getIfPresent(K key);

    Optional<V> get(K key, Supplier<V> loaderOverride);

    default Map<K, Optional<V>> getAll(Collection<K> keys) {
        Map<K, Optional<V>> result = new LinkedHashMap<>();
        for (K key : keys) {
            result.put(key, get(key));
        }
        return result;
    }

    Map<K, Optional<V>> getAll(Collection<K> keys, Function<Collection<K>, Map<K, V>> batchLoader);

    default V getOrThrow(K key) {
        return get(key).orElseThrow(() ->
                new NoSuchElementException("Cache miss: " + name() + " key=" + key));
    }

    void put(K key, V value);

    default void putAll(Map<K, V> values) {
        values.forEach(this::put);
    }

    void invalidate(K key);

    default void invalidateAll(Collection<K> keys) {
        keys.forEach(this::invalidate);
    }

    default void warmup(Collection<K> keys) {
        keys.forEach(this::get);
    }

    TieredCacheStats stats();

    void safeWrite(K key, Runnable writeAction, long delayMs);

    void refresh(K key);

    ReactiveTieredCache<K, V> asReactive();
}
