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
    /**
     * 查询或读取 get 相关的业务数据。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    Mono<Optional<V>> get(K key);

    /**
     * 查询或读取 get If Present 相关的业务数据。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    Mono<Optional<V>> getIfPresent(K key);

    /**
     * 查询或读取 get All 相关的业务数据。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @param keys keys 对应的缓存键、配置键或业务键
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    default Mono<Map<K, Optional<V>>> getAll(Collection<K> keys) {
        Map<K, Optional<V>> result = new LinkedHashMap<>();
        Mono<Map<K, Optional<V>>> chain = Mono.just(result);
        for (K key : keys) {
            // 按顺序串联每个 key 的异步读取，保证返回 Map 的插入顺序与入参一致。
            chain = chain.flatMap(values -> get(key).map(value -> {
                values.put(key, value);
                return values;
            }));
        }
        return chain;
    }

    /**
     * 写入或记录 put 相关的业务数据。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param value value 待写入、比较或转换的业务值
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    Mono<Void> put(K key, V value);

    /**
     * 删除、清理或失效 invalidate 相关的业务数据。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    Mono<Void> invalidate(K key);

    /**
     * 更新或刷新 refresh 相关的业务数据。
     *
     * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    Mono<Void> refresh(K key);
}
