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
     * 创建只使用 Redis Pub/Sub 的缓存管理器。
     *
     * <p>该构造器适合单集群部署场景，跨节点 L1 失效通过 Redis 频道广播完成。
     *
     * @param redis 同步 Redis 模板，用于 L2 读写和失效消息发布
     * @param reactiveRedis 响应式 Redis 模板，用于响应式缓存视图
     * @param meterRegistry 缓存指标注册表
     * @param reactiveFactory 响应式 Redis 连接工厂，用于订阅失效频道
     */
    public TieredCacheManager(StringRedisTemplate redis,
                              ReactiveStringRedisTemplate reactiveRedis,
                              MeterRegistry meterRegistry,
                              ReactiveRedisConnectionFactory reactiveFactory) {
        this(redis, reactiveRedis, meterRegistry, reactiveFactory, List.of());
    }

    /**
     * 创建带外部失效发布器的缓存管理器。
     *
     * <p>Redis Pub/Sub 负责当前 Redis 集群内的 L1 失效；外部发布器可桥接 MQ 或事件总线实现跨集群失效。
     *
     * @param redis 同步 Redis 模板，用于 L2 读写和失效消息发布
     * @param reactiveRedis 响应式 Redis 模板，用于响应式缓存视图
     * @param meterRegistry 缓存指标注册表
     * @param reactiveFactory 响应式 Redis 连接工厂，用于订阅失效频道
     * @param externalInvalidationPublishers 额外的失效事件发布器集合
     */
    public TieredCacheManager(StringRedisTemplate redis,
                              ReactiveStringRedisTemplate reactiveRedis,
                              MeterRegistry meterRegistry,
                              ReactiveRedisConnectionFactory reactiveFactory,
                              Collection<CacheInvalidationPublisher> externalInvalidationPublishers) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        this.redis = redis;
        this.reactiveRedis = reactiveRedis;
        this.meterRegistry = meterRegistry;
        this.reactiveFactory = reactiveFactory;
        this.externalInvalidationPublishers = externalInvalidationPublishers == null
                ? List.of()
                : List.copyOf(externalInvalidationPublishers);
    }

    /**
     * 启动 Redis 失效频道订阅。
     *
     * <p>该订阅只消费 {@code tiered-cache:*:invalidate} 消息并清理本节点 L1，不会重新发布事件。
     * <p>订阅失败只记录告警，缓存仍可依赖本节点失效和 L2 TTL 继续工作。
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            log.warn("[TIERED_CACHE_MANAGER] Pub/Sub subscribe failed: {}", e.getMessage());
        }
    }

    /**
     * 关闭 Redis 失效频道订阅。
     *
     * <p>应用退出时释放响应式订阅和监听容器；关闭失败只记录 debug 日志，避免影响 Spring 销毁流程。
     */
    @PreDestroy
    void closeInvalidationSubscription() {
        if (invalidationSubscription != null && !invalidationSubscription.isDisposed()) {
            invalidationSubscription.dispose();
        }
        if (listenerContainer != null) {
            try {
                listenerContainer.destroy();
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (Exception e) {
                log.debug("[TIERED_CACHE_MANAGER] Pub/Sub container destroy ignored: {}", e.getMessage());
            }
        }
    }

    /**
     * 注册一个已经构建完成的缓存实例。
     *
     * <p>注册后注解切面可以按 cacheName 定位缓存，跨节点失效事件也能路由到对应实例。
     *
     * @param cache 需要注册的缓存实现
     */
    public void register(TieredCacheImpl<?, ?> cache) {
        caches.put(cache.getName(), cache);
        log.info("[TIERED_CACHE_MANAGER] registered cache name={}", cache.getName());
    }

    /**
     * 按名称查找已注册的缓存实例。
     *
     * <p>该方法不会自动创建缓存；未注册时返回空，通常由注解切面据此报出配置错误。
     *
     * @param name 缓存实例名称
     * @return 已注册缓存实例
     */
    public Optional<TieredCache<?, ?>> getCache(String name) {
        return Optional.ofNullable(caches.get(name));
    }

    /**
     * 发布缓存失效事件。
     *
     * <p>事件会先通过 Redis Pub/Sub 通知同集群节点，再交给外部发布器桥接其他通道。
     * <p>单个外部发布器失败只记录告警，不阻断其他发布器和当前缓存写入流程。
     *
     * @param event 缓存失效事件
     */
    public void publish(CacheInvalidationEvent event) {
        // Redis Pub/Sub 用于同集群内本地缓存失效，外部 publisher 用于桥接 MQ 等跨进程通道。
        publishRedisInvalidation(event.cacheName(), event.rawKey());
        for (CacheInvalidationPublisher publisher : externalInvalidationPublishers) {
            try {
                publisher.publish(event);
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (Exception e) {
                log.warn("[TIERED_CACHE_MANAGER] external invalidation publish failed cache={} key={}: {}",
                        event.cacheName(), event.rawKey(), e.getMessage());
            }
        }
    }

    /**
     * 接收并应用缓存失效事件。
     *
     * <p>该方法只清理本节点对应缓存的 L1 数据，不删除 Redis，也不再次广播，避免事件循环。
     *
     * @param event 缓存失效事件
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
     * 通过 Redis Pub/Sub 发布本地缓存失效消息。
     *
     * <p>发布失败只记录告警，不阻断当前写入或失效流程；其他节点仍可依赖 L2 TTL 最终收敛。
     *
     * @param cacheName 缓存实例名称
     * @param rawKey 原始业务缓存 key
     */
    private void publishRedisInvalidation(String cacheName, String rawKey) {
        try {
            redis.convertAndSend(invalidateChannel(cacheName), rawKey);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            log.warn("[TIERED_CACHE_MANAGER] Pub/Sub publish failed cache={} key={}: {}",
                    cacheName, rawKey, e.getMessage());
        }
    }

    /**
     * 生成 Redis 本地缓存失效频道名。
     *
     * <p>频道名携带 cacheName，消息体携带 rawKey，订阅端据此定位并清理本节点 L1。
     *
     * @param cacheName 缓存实例名称
     * @return Redis Pub/Sub 频道名
     */
    private String invalidateChannel(String cacheName) {
        return "tiered-cache:" + cacheName + ":invalidate";
    }
}
