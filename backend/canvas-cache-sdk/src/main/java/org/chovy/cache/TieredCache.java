package org.chovy.cache;

import java.util.NoSuchElementException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

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
