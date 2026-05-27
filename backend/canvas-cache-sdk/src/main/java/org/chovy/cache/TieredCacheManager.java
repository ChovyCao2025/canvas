package org.chovy.cache;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import reactor.core.Disposable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分层缓存注册中心，按缓存名称保存并查找 TieredCache 实例。
 *
 * <p>业务模块通过管理器复用已构建的缓存，避免重复创建同名缓存导致策略和统计口径不一致。
 * <p>该类只负责缓存实例生命周期管理，不参与具体 key 的读写逻辑。
 */
@Slf4j
public class TieredCacheManager {
    /** 同步 Redis 操作模板。 */
    @Getter private final StringRedisTemplate redis;
    /** 响应式 Redis 操作模板。 */
    @Getter private final ReactiveStringRedisTemplate reactiveRedis;
    /** 缓存指标注册表。 */
    @Getter private final MeterRegistry meterRegistry;
    /** 响应式 Redis 连接工厂。 */
    private final ReactiveRedisConnectionFactory reactiveFactory;
    /** 外部缓存失效事件发布器集合。 */
    private final List<CacheInvalidationPublisher> externalInvalidationPublishers;
    /** 按缓存名称索引的已注册缓存实例。 */
    private final Map<String, TieredCacheImpl<?, ?>> caches = new ConcurrentHashMap<>();
    /** Redis 缓存失效消息监听容器。 */
    private ReactiveRedisMessageListenerContainer listenerContainer;
    /** 缓存失效消息订阅句柄。 */
    private Disposable invalidationSubscription;

    /**
     * 构造 TieredCacheManager 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param redis redis 方法执行所需的业务参数
     * @param reactiveRedis reactiveRedis 方法执行所需的业务参数
     * @param meterRegistry meterRegistry 方法执行所需的业务参数
     * @param reactiveFactory reactiveFactory 方法执行所需的业务参数
     */
    public TieredCacheManager(StringRedisTemplate redis,
                              ReactiveStringRedisTemplate reactiveRedis,
                              MeterRegistry meterRegistry,
                              ReactiveRedisConnectionFactory reactiveFactory) {
        this(redis, reactiveRedis, meterRegistry, reactiveFactory, List.of());
    }

    /**
     * 构造 TieredCacheManager 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param redis redis 方法执行所需的业务参数
     * @param reactiveRedis reactiveRedis 方法执行所需的业务参数
     * @param meterRegistry meterRegistry 方法执行所需的业务参数
     * @param reactiveFactory reactiveFactory 方法执行所需的业务参数
     * @param externalInvalidationPublishers externalInvalidationPublishers 方法执行所需的业务参数
     */
    public TieredCacheManager(StringRedisTemplate redis,
                              ReactiveStringRedisTemplate reactiveRedis,
                              MeterRegistry meterRegistry,
                              ReactiveRedisConnectionFactory reactiveFactory,
                              Collection<CacheInvalidationPublisher> externalInvalidationPublishers) {
        this.redis = redis;
        this.reactiveRedis = reactiveRedis;
        this.meterRegistry = meterRegistry;
        this.reactiveFactory = reactiveFactory;
        this.externalInvalidationPublishers = externalInvalidationPublishers == null
                ? List.of()
                : List.copyOf(externalInvalidationPublishers);
    }

    /**
     * 执行 subscribe Invalidations 对应的业务逻辑。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     */
    @PostConstruct
    void subscribeInvalidations() {
        if (reactiveFactory == null) {
            return;
        }
        try {
            listenerContainer = new ReactiveRedisMessageListenerContainer(reactiveFactory);
            invalidationSubscription = listenerContainer.receive(PatternTopic.of("tiered-cache:*:invalidate"))
                    .subscribe(msg -> {
                        String[] parts = msg.getChannel().split(":");
                        if (parts.length >= 3) {
                            // 频道名携带 cacheName，消息体携带原始 key，收到后只清理本节点对应缓存实例。
                            receiveInvalidation(new CacheInvalidationEvent(parts[1], msg.getMessage(), 0L));
                        }
                    }, e -> log.error("[TIERED_CACHE_MANAGER] Pub/Sub error: {}", e.getMessage()));
            log.info("[TIERED_CACHE_MANAGER] subscribed tiered-cache:*:invalidate");
        } catch (Exception e) {
            log.warn("[TIERED_CACHE_MANAGER] Pub/Sub subscribe failed: {}", e.getMessage());
        }
    }

    /**
     * 停止或关闭 close Invalidation Subscription 相关的业务数据。
     *
     * <p>实现会处理 MQ 消息、路由或发送记录，影响异步触发链路。
     */
    @PreDestroy
    void closeInvalidationSubscription() {
        if (invalidationSubscription != null && !invalidationSubscription.isDisposed()) {
            invalidationSubscription.dispose();
        }
        if (listenerContainer != null) {
            try {
                listenerContainer.destroy();
            } catch (Exception e) {
                log.debug("[TIERED_CACHE_MANAGER] Pub/Sub container destroy ignored: {}", e.getMessage());
            }
        }
    }

    /**
     * 注册、调度或初始化 register 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param cache cache 方法执行所需的业务参数
     */
    public void register(TieredCacheImpl<?, ?> cache) {
        caches.put(cache.getName(), cache);
        log.info("[TIERED_CACHE_MANAGER] registered cache name={}", cache.getName());
    }

    /**
     * 查询或读取 get Cache 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param name name 方法执行所需的业务参数
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
    public Optional<TieredCache<?, ?>> getCache(String name) {
        return Optional.ofNullable(caches.get(name));
    }

    /**
     * 发布或发送 publish 相关的业务数据。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param event event 方法执行所需的业务参数
     */
    public void publish(CacheInvalidationEvent event) {
        // Redis Pub/Sub 用于同集群内本地缓存失效，外部 publisher 用于桥接 MQ 等跨进程通道。
        publishRedisInvalidation(event.cacheName(), event.rawKey());
        for (CacheInvalidationPublisher publisher : externalInvalidationPublishers) {
            try {
                publisher.publish(event);
            } catch (Exception e) {
                log.warn("[TIERED_CACHE_MANAGER] external invalidation publish failed cache={} key={}: {}",
                        event.cacheName(), event.rawKey(), e.getMessage());
            }
        }
    }

    /**
     * 执行 receive Invalidation 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param event event 方法执行所需的业务参数
     */
    public void receiveInvalidation(CacheInvalidationEvent event) {
        if (event == null || event.cacheName() == null) {
            return;
        }
        TieredCacheImpl<?, ?> cache = caches.get(event.cacheName());
        if (cache != null) {
            // 广播事件只触发本地 L1 清理，不再次发布，避免失效消息在节点间循环传播。
            cache.onInvalidateBroadcast(event.rawKey());
        }
    }

    /**
     * 发布或发送 publish Redis Invalidation 相关的业务数据。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param cacheName cacheName 方法执行所需的业务参数
     * @param rawKey rawKey 对应的缓存键、配置键或业务键
     */
    private void publishRedisInvalidation(String cacheName, String rawKey) {
        try {
            redis.convertAndSend(invalidateChannel(cacheName), rawKey);
        } catch (Exception e) {
            log.warn("[TIERED_CACHE_MANAGER] Pub/Sub publish failed cache={} key={}: {}",
                    cacheName, rawKey, e.getMessage());
        }
    }

    /**
     * 删除、清理或失效 invalidate Channel 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param cacheName cacheName 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String invalidateChannel(String cacheName) {
        return "tiered-cache:" + cacheName + ":invalidate";
    }
}
