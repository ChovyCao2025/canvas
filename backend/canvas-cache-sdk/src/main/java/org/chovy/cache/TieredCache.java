package org.chovy.cache;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

public interface TieredCache<K, V> {
    String name();

    Optional<V> get(K key);

    Optional<V> get(K key, Supplier<V> loaderOverride);

    default V getOrThrow(K key) {
        return get(key).orElseThrow(() ->
                new NoSuchElementException("Cache miss: " + name() + " key=" + key));
    }

    void put(K key, V value);

    void invalidate(K key);

    void safeWrite(K key, Runnable writeAction, long delayMs);

    void refresh(K key);

    ReactiveTieredCache<K, V> asReactive();
}
