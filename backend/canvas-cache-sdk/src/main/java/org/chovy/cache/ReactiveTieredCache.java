package org.chovy.cache;

import reactor.core.publisher.Mono;

import java.util.Optional;

public interface ReactiveTieredCache<K, V> {
    Mono<Optional<V>> get(K key);

    Mono<Void> put(K key, V value);

    Mono<Void> invalidate(K key);

    Mono<Void> refresh(K key);
}
