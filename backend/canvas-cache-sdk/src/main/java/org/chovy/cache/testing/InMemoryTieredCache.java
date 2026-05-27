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
    private final String name;
    private final Function<K, V> loader;
    private final ConcurrentHashMap<K, Optional<V>> store = new ConcurrentHashMap<>();
    private final LongAdder hits = new LongAdder();
    private final LongAdder misses = new LongAdder();

    private InMemoryTieredCache(String name, Function<K, V> loader) {
        this.name = name;
        this.loader = loader;
    }

    public static <K, V> InMemoryTieredCache<K, V> of(Function<K, V> loader) {
        return new InMemoryTieredCache<>("test", loader);
    }

    public static <K, V> InMemoryTieredCache<K, V> of(String name, Function<K, V> loader) {
        return new InMemoryTieredCache<>(name, loader);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<V> get(K key) {
        Optional<V> existing = store.get(key);
        if (existing != null) {
            hits.increment();
            return existing;
        }
        misses.increment();
        Optional<V> loaded = Optional.ofNullable(loader.apply(key));
        store.put(key, loaded);
        return loaded;
    }

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

    @Override
    public Optional<V> get(K key, Supplier<V> loaderOverride) {
        return store.computeIfAbsent(key, ignored -> Optional.ofNullable(loaderOverride.get()));
    }

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
        Map<K, V> loaded = misses.isEmpty() ? Map.of() : batchLoader.apply(misses);
        for (K key : misses) {
            Optional<V> value = Optional.ofNullable(loaded.get(key));
            store.put(key, value);
            result.put(key, value);
        }
        return result;
    }

    @Override
    public void put(K key, V value) {
        store.put(key, Optional.ofNullable(value));
    }

    @Override
    public void invalidate(K key) {
        store.remove(key);
    }

    @Override
    public void safeWrite(K key, Runnable writeAction, long delayMs) {
        invalidate(key);
        writeAction.run();
        invalidate(key);
    }

    @Override
    public void refresh(K key) {
        invalidate(key);
        get(key);
    }

    @Override
    public ReactiveTieredCache<K, V> asReactive() {
        return new ReactiveTieredCache<>() {
            @Override
            public Mono<Optional<V>> get(K key) {
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
