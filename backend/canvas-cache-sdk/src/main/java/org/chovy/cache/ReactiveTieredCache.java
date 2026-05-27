package org.chovy.cache;

import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 响应式分层缓存接口，用于 WebFlux 或 Reactor 链路中以 Mono 形式访问缓存。
 *
 * <p>该接口保持与同步 TieredCache 相同的命中、回填、失效语义，但避免阻塞调用线程。
 * <p>实现类需要负责将底层缓存、加载器和异常处理桥接到响应式发布者中。
 */
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
