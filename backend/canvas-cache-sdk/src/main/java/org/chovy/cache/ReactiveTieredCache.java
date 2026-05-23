package org.chovy.cache;

import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public interface ReactiveTieredCache<K, V> {
    Mono<Optional<V>> get(K key);

    Mono<Optional<V>> getIfPresent(K key);

    default Mono<Map<K, Optional<V>>> getAll(Collection<K> keys) {
        Map<K, Optional<V>> result = new LinkedHashMap<>();
        Mono<Map<K, Optional<V>>> chain = Mono.just(result);
        for (K key : keys) {
            chain = chain.flatMap(values -> get(key).map(value -> {
                values.put(key, value);
                return values;
            }));
        }
        return chain;
    }

    Mono<Void> put(K key, V value);

    Mono<Void> invalidate(K key);

    Mono<Void> refresh(K key);
}
