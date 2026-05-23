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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TieredCacheManager {
    @Getter private final StringRedisTemplate redis;
    @Getter private final ReactiveStringRedisTemplate reactiveRedis;
    @Getter private final MeterRegistry meterRegistry;
    private final ReactiveRedisConnectionFactory reactiveFactory;
    private final Map<String, TieredCacheImpl<?, ?>> caches = new ConcurrentHashMap<>();
    private ReactiveRedisMessageListenerContainer listenerContainer;
    private Disposable invalidationSubscription;

    public TieredCacheManager(StringRedisTemplate redis,
                              ReactiveStringRedisTemplate reactiveRedis,
                              MeterRegistry meterRegistry,
                              ReactiveRedisConnectionFactory reactiveFactory) {
        this.redis = redis;
        this.reactiveRedis = reactiveRedis;
        this.meterRegistry = meterRegistry;
        this.reactiveFactory = reactiveFactory;
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
                            TieredCacheImpl<?, ?> cache = caches.get(parts[1]);
                            if (cache != null) {
                                cache.onInvalidateBroadcast(msg.getMessage());
                            }
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
}
