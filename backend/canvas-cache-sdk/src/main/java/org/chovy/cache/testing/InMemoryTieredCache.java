package org.chovy.cache.testing;

import org.chovy.cache.ReactiveTieredCache;
import org.chovy.cache.TieredCache;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class InMemoryTieredCache<K, V> implements TieredCache<K, V> {
    private final String name;
    private final Function<K, V> loader;
    private final ConcurrentHashMap<K, Optional<V>> store = new ConcurrentHashMap<>();

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
        return store.computeIfAbsent(key, ignored -> Optional.ofNullable(loader.apply(key)));
    }

    @Override
    public Optional<V> get(K key, Supplier<V> loaderOverride) {
        return store.computeIfAbsent(key, ignored -> Optional.ofNullable(loaderOverride.get()));
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
}
