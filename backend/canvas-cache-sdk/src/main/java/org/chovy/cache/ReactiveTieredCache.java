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
     * 响应式读取单个 key，并在订阅时执行完整 L1/L2/L3 缓存链路。
     *
     * <p>返回的 {@link Mono} 以 {@link Optional} 表达业务空值，缓存失败和加载失败通过响应式错误信号传递。
     *
     * @param key 业务缓存 key
     * @return 订阅后产生的缓存读取结果
     */
    Mono<Optional<V>> get(K key);

    /**
     * 响应式读取已存在的缓存值，不触发 L3 回源加载。
     *
     * <p>适合在响应式链路中做非侵入式缓存探测；缓存未命中会返回 {@link Optional#empty()}。
     *
     * @param key 业务缓存 key
     * @return 订阅后产生的已缓存值
     */
    Mono<Optional<V>> getIfPresent(K key);

    /**
     * 响应式批量读取多个 key。
     *
     * <p>默认实现按输入顺序串联每个 key 的 {@link #get(Object)}，便于保持返回 Map 的稳定顺序。
     *
     * @param keys 需要读取的业务缓存 key 集合
     * @return 订阅后产生的 key 到缓存值映射
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
     * 响应式写入单个缓存值。
     *
     * <p>订阅后才执行写入；实现应保持与同步 put 相同的 L1/L2 更新和本地缓存失效广播语义。
     *
     * @param key 业务缓存 key
     * @param value 待缓存的业务值
     * @return 写入完成信号
     */
    Mono<Void> put(K key, V value);

    /**
     * 响应式失效单个缓存 key。
     *
     * <p>订阅后执行 L1 清理、L2 删除和跨节点失效通知；失败通过错误信号返回给调用方。
     *
     * @param key 需要失效的业务缓存 key
     * @return 失效完成信号
     */
    Mono<Void> invalidate(K key);

    /**
     * 响应式刷新单个 key 的缓存值。
     *
     * <p>订阅后重新进入加载和回填流程；加载失败时按底层缓存实现的失败策略转换为完成或错误信号。
     *
     * @param key 需要刷新的业务缓存 key
     * @return 刷新完成信号
     */
    Mono<Void> refresh(K key);
}
