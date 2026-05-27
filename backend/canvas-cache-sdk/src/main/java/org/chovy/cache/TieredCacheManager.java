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
    @Getter private final StringRedisTemplate redis;
    @Getter private final ReactiveStringRedisTemplate reactiveRedis;
    @Getter private final MeterRegistry meterRegistry;
    private final ReactiveRedisConnectionFactory reactiveFactory;
    private final List<CacheInvalidationPublisher> externalInvalidationPublishers;
    private final Map<String, TieredCacheImpl<?, ?>> caches = new ConcurrentHashMap<>();
    private ReactiveRedisMessageListenerContainer listenerContainer;
    private Disposable invalidationSubscription;

    public TieredCacheManager(StringRedisTemplate redis,
                              ReactiveStringRedisTemplate reactiveRedis,
                              MeterRegistry meterRegistry,
                              ReactiveRedisConnectionFactory reactiveFactory) {
        this(redis, reactiveRedis, meterRegistry, reactiveFactory, List.of());
    }

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
                            receiveInvalidation(new CacheInvalidationEvent(parts[1], msg.getMessage(), 0L));
                        }
                    }, e -> log.error("[TIERED_CACHE_MANAGER] Pub/Sub error: {}", e.getMessage()));
            log.info("[TIERED_CACHE_MANAGER] subscribed tiered-cache:*:invalidate");
        } catch (Exception e) {
            log.warn("[TIERED_CACHE_MANAGER] Pub/Sub subscribe failed: {}", e.getMessage());
        }
    }

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

    public void register(TieredCacheImpl<?, ?> cache) {
        caches.put(cache.getName(), cache);
        log.info("[TIERED_CACHE_MANAGER] registered cache name={}", cache.getName());
    }

    public Optional<TieredCache<?, ?>> getCache(String name) {
        return Optional.ofNullable(caches.get(name));
    }

    public void publish(CacheInvalidationEvent event) {
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

    public void receiveInvalidation(CacheInvalidationEvent event) {
        if (event == null || event.cacheName() == null) {
            return;
        }
        TieredCacheImpl<?, ?> cache = caches.get(event.cacheName());
        if (cache != null) {
            cache.onInvalidateBroadcast(event.rawKey());
        }
    }

    private void publishRedisInvalidation(String cacheName, String rawKey) {
        try {
            redis.convertAndSend(invalidateChannel(cacheName), rawKey);
        } catch (Exception e) {
            log.warn("[TIERED_CACHE_MANAGER] Pub/Sub publish failed cache={} key={}: {}",
                    cacheName, rawKey, e.getMessage());
        }
    }

    private String invalidateChannel(String cacheName) {
        return "tiered-cache:" + cacheName + ":invalidate";
    }
}
