package org.chovy.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.chovy.cache.strategy.DeserializeFailureStrategy;
import org.chovy.cache.strategy.LoaderFailureStrategy;
import org.chovy.cache.strategy.RedisFailureStrategy;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class TieredCacheImpl<K, V> implements TieredCache<K, V> {
    static final String NULL_SENTINEL = "__NULL__";

    @Getter private final String name;
    private final String l2KeyPrefix;
    private final Duration l2Ttl;
    private final double l2TtlJitter;
    private final int keySchemaVersion;
    private final Duration nullValueTtl;
    private final boolean hotspotProtection;
    private final LoaderFailureStrategy loaderFailure;
    private final RedisFailureStrategy redisReadFailure;
    private final RedisFailureStrategy redisWriteFailure;
    private final DeserializeFailureStrategy deserializeFailure;
    private final Function<K, V> loader;
    private final JavaType valueJavaType;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redis;
    @SuppressWarnings("unused")
    private final ReactiveStringRedisTemplate reactiveRedis;
    final LoadingCache<K, Optional<V>> l1;
    private final ReactiveTieredCache<K, V> reactiveView;

    private Counter l1Hits;
    private Counter l2Hits;
    private Counter l3Hits;
    private Counter l1Misses;
    private Counter l2Misses;
    private Timer loadTimer;
    private Counter penetrationHits;
    private Counter hotspotWaits;
    private Counter loaderFailures;

    TieredCacheImpl(String name,
                    int l1MaxSize,
                    Duration l1RefreshAfterWrite,
                    String l2KeyPrefix,
                    Duration l2Ttl,
                    double l2TtlJitter,
                    int keySchemaVersion,
                    Duration nullValueTtl,
                    boolean hotspotProtection,
                    LoaderFailureStrategy loaderFailure,
                    RedisFailureStrategy redisReadFailure,
                    RedisFailureStrategy redisWriteFailure,
                    DeserializeFailureStrategy deserializeFailure,
                    Function<K, V> loader,
                    JavaType valueJavaType,
                    ObjectMapper objectMapper,
                    StringRedisTemplate redis,
                    ReactiveStringRedisTemplate reactiveRedis,
                    MeterRegistry meterRegistry) {
        this.name = name;
        this.l2KeyPrefix = l2KeyPrefix;
        this.l2Ttl = l2Ttl;
        this.l2TtlJitter = l2TtlJitter;
        this.keySchemaVersion = keySchemaVersion;
        this.nullValueTtl = nullValueTtl;
        this.hotspotProtection = hotspotProtection;
        this.loaderFailure = loaderFailure;
        this.redisReadFailure = redisReadFailure;
        this.redisWriteFailure = redisWriteFailure;
        this.deserializeFailure = deserializeFailure;
        this.loader = loader;
        this.valueJavaType = valueJavaType;
        this.objectMapper = objectMapper;
        this.redis = redis;
        this.reactiveRedis = reactiveRedis;
        this.l1 = Caffeine.newBuilder()
                .maximumSize(l1MaxSize)
                .refreshAfterWrite(l1RefreshAfterWrite)
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build(buildCacheLoader());
        this.reactiveView = new ReactiveTieredCacheView<>(this);
        if (meterRegistry != null) {
            registerMetrics(meterRegistry);
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<V> get(K key) {
        Optional<V> cached = l1.getIfPresent(key);
        if (cached != null) {
            countHit("L1");
            return l1.get(key);
        }
        countMiss("L1");
        Optional<V> loaded = l1.get(key);
        return loaded == null ? Optional.empty() : loaded;
    }

    @Override
    public Optional<V> get(K key, Supplier<V> loaderOverride) {
        Optional<V> cached = l1.getIfPresent(key);
        if (cached != null) {
            countHit("L1");
            return cached;
        }
        countMiss("L1");
        Optional<V> loaded = loadFromL2ThenL3(key, loaderOverride);
        l1.put(key, loaded);
        return loaded;
    }

    @Override
    public void put(K key, V value) {
        if (writeL2(key, value)) {
            l1.put(key, Optional.ofNullable(value));
        }
    }

    @Override
    public void invalidate(K key) {
        l1.invalidate(key);
        deleteL2(key);
        publishInvalidate(keyToString(key));
        log.debug("[TIERED_CACHE][{}] invalidate key={}", name, key);
    }

    @Override
    public void safeWrite(K key, Runnable writeAction, long delayMs) {
        l1.invalidate(key);
        deleteL2(key);
        publishInvalidate(keyToString(key));
        try {
            writeAction.run();
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        l1.invalidate(key);
        deleteL2(key);
        publishInvalidate(keyToString(key));
    }

    @Override
    public void refresh(K key) {
        l1.invalidate(key);
        deleteL2(key);
        Optional<V> loaded = loadFromL2ThenL3(key, () -> loader.apply(key));
        l1.put(key, loaded);
    }

    @Override
    public ReactiveTieredCache<K, V> asReactive() {
        return reactiveView;
    }

    void onInvalidateBroadcast(String rawKey) {
        l1.asMap().keySet().removeIf(key -> keyToString(key).equals(rawKey));
        log.debug("[TIERED_CACHE][{}] L1 evicted from pub/sub rawKey={}", name, rawKey);
    }

    private CacheLoader<K, Optional<V>> buildCacheLoader() {
        return new CacheLoader<>() {
            @Override
            public Optional<V> load(K key) {
                return loadFromL2ThenL3(key, () -> loader.apply(key));
            }

            @Override
            public Optional<V> reload(K key, Optional<V> oldValue) {
                try {
                    String json = redis.opsForValue().get(l2Key(key));
                    if (json != null) {
                        if (NULL_SENTINEL.equals(json)) {
                            return Optional.empty();
                        }
                        return Optional.ofNullable(deserialize(json));
                    }
                } catch (Exception e) {
                    if (redisReadFailure == RedisFailureStrategy.FAIL_FAST) {
                        throw asRuntime(e);
                    }
                    log.warn("[TIERED_CACHE][{}] Redis read failed on refresh: {}", name, e.getMessage());
                }
                return loadFromL3(key, oldValue, () -> loader.apply(key));
            }
        };
    }

    private Optional<V> loadFromL2ThenL3(K key, Supplier<V> effectiveLoader) {
        try {
            String json = redis.opsForValue().get(l2Key(key));
            if (json != null) {
                if (NULL_SENTINEL.equals(json)) {
                    countHit("L2");
                    increment(penetrationHits);
                    return Optional.empty();
                }
                V value = deserializeWithFallback(key, json);
                if (value != null) {
                    countHit("L2");
                    return Optional.of(value);
                }
            }
            countMiss("L2");
        } catch (Exception e) {
            if (redisReadFailure == RedisFailureStrategy.FAIL_FAST) {
                throw asRuntime(e);
            }
            log.warn("[TIERED_CACHE][{}] Redis read failed, fallthrough to L3: {}", name, e.getMessage());
        }
        return hotspotProtection
                ? loadFromL3WithLock(key, null, effectiveLoader)
                : loadFromL3(key, null, effectiveLoader);
    }

    private Optional<V> loadFromL3(K key, Optional<V> staleValue, Supplier<V> effectiveLoader) {
        try {
            countHit("L3");
            V value = loadTimer != null ? loadTimer.record(effectiveLoader::get) : effectiveLoader.get();
            writeL2(key, value);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            increment(loaderFailures);
            return switch (loaderFailure) {
                case THROW -> throw asRuntime(e);
                case RETURN_STALE -> staleValue != null ? staleValue : Optional.empty();
                case RETURN_EMPTY -> Optional.empty();
            };
        }
    }

    private Optional<V> loadFromL3WithLock(K key, Optional<V> staleValue, Supplier<V> effectiveLoader) {
        String lockKey = "cache:lock:" + l2Key(key);
        boolean locked = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(30)));
        if (locked) {
            try {
                return loadFromL3(key, staleValue, effectiveLoader);
            } finally {
                redis.delete(lockKey);
            }
        }
        return waitAndRetryL2(key, staleValue, effectiveLoader);
    }

    private Optional<V> waitAndRetryL2(K key, Optional<V> staleValue, Supplier<V> effectiveLoader) {
        increment(hotspotWaits);
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(50);
                String json = redis.opsForValue().get(l2Key(key));
                if (json != null) {
                    if (NULL_SENTINEL.equals(json)) {
                        return Optional.empty();
                    }
                    return Optional.ofNullable(deserializeWithFallback(key, json));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ignored) {
                break;
            }
        }
        return loadFromL3(key, staleValue, effectiveLoader);
    }

    private boolean writeL2(K key, V value) {
        try {
            if (value == null) {
                redis.opsForValue().set(l2Key(key), NULL_SENTINEL, nullValueTtl);
            } else {
                redis.opsForValue().set(l2Key(key), serialize(value), actualL2Ttl());
            }
            return true;
        } catch (Exception e) {
            if (redisWriteFailure == RedisFailureStrategy.FAIL_FAST) {
                throw asRuntime(e);
            }
            log.warn("[TIERED_CACHE][{}] Redis write failed: {}", name, e.getMessage());
            return false;
        }
    }

    private void deleteL2(K key) {
        try {
            redis.delete(l2Key(key));
        } catch (Exception e) {
            if (redisWriteFailure == RedisFailureStrategy.FAIL_FAST) {
                throw asRuntime(e);
            }
            log.warn("[TIERED_CACHE][{}] Redis delete failed: {}", name, e.getMessage());
        }
    }

    private void publishInvalidate(String rawKey) {
        try {
            redis.convertAndSend(invalidateChannel(), rawKey);
        } catch (Exception e) {
            log.warn("[TIERED_CACHE][{}] Pub/Sub publish failed: {}", name, e.getMessage());
        }
    }

    String invalidateChannel() {
        return "tiered-cache:" + name + ":invalidate";
    }

    String l2Key(K key) {
        return l2KeyPrefix + "v" + keySchemaVersion + ":" + keyToString(key);
    }

    private String keyToString(K key) {
        return String.valueOf(key);
    }

    private Duration actualL2Ttl() {
        if (l2TtlJitter <= 0) {
            return l2Ttl;
        }
        long jitterMillis = (long) (l2Ttl.toMillis()
                * ThreadLocalRandom.current().nextDouble(0, l2TtlJitter));
        return l2Ttl.plus(Duration.ofMillis(jitterMillis));
    }

    private String serialize(V value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Cache serialize failed", e);
        }
    }

    private V deserialize(String json) {
        try {
            return objectMapper.readValue(json, valueJavaType);
        } catch (Exception e) {
            throw new IllegalStateException("Cache deserialize failed", e);
        }
    }

    private V deserializeWithFallback(K key, String json) {
        try {
            return objectMapper.readValue(json, valueJavaType);
        } catch (Exception e) {
            if (deserializeFailure == DeserializeFailureStrategy.THROW) {
                throw new IllegalStateException("Cache deserialize failed", e);
            }
            log.warn("[TIERED_CACHE][{}] deserialize failed, deleting L2 key={}: {}", name, key, e.getMessage());
            deleteL2(key);
            return null;
        }
    }

    private RuntimeException asRuntime(Exception e) {
        return e instanceof RuntimeException runtime ? runtime : new RuntimeException(e);
    }

    private void registerMetrics(MeterRegistry registry) {
        Tags tags = Tags.of("cache", name);
        l1Hits = registry.counter("tiered_cache_hits_total", tags.and("level", "L1"));
        l2Hits = registry.counter("tiered_cache_hits_total", tags.and("level", "L2"));
        l3Hits = registry.counter("tiered_cache_hits_total", tags.and("level", "L3"));
        l1Misses = registry.counter("tiered_cache_misses_total", tags.and("level", "L1"));
        l2Misses = registry.counter("tiered_cache_misses_total", tags.and("level", "L2"));
        loadTimer = registry.timer("tiered_cache_load_duration_seconds", tags);
        penetrationHits = registry.counter("tiered_cache_penetration_total", tags);
        hotspotWaits = registry.counter("tiered_cache_hotspot_lock_wait_total", tags);
        loaderFailures = registry.counter("tiered_cache_loader_failure_total", tags.and("strategy", loaderFailure.name()));
    }

    private void countHit(String level) {
        if ("L1".equals(level)) increment(l1Hits);
        if ("L2".equals(level)) increment(l2Hits);
        if ("L3".equals(level)) increment(l3Hits);
    }

    private void countMiss(String level) {
        if ("L1".equals(level)) increment(l1Misses);
        if ("L2".equals(level)) increment(l2Misses);
    }

    private void increment(Counter counter) {
        if (counter != null) {
            counter.increment();
        }
    }

    private static class ReactiveTieredCacheView<K, V> implements ReactiveTieredCache<K, V> {
        private final TieredCacheImpl<K, V> delegate;

        ReactiveTieredCacheView(TieredCacheImpl<K, V> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Mono<Optional<V>> get(K key) {
            return Mono.fromCallable(() -> delegate.get(key)).subscribeOn(Schedulers.boundedElastic());
        }

        @Override
        public Mono<Void> put(K key, V value) {
            return Mono.fromRunnable(() -> delegate.put(key, value)).subscribeOn(Schedulers.boundedElastic()).then();
        }

        @Override
        public Mono<Void> invalidate(K key) {
            return Mono.fromRunnable(() -> delegate.invalidate(key)).subscribeOn(Schedulers.boundedElastic()).then();
        }

        @Override
        public Mono<Void> refresh(K key) {
            return Mono.fromRunnable(() -> delegate.refresh(key)).subscribeOn(Schedulers.boundedElastic()).then();
        }
    }
}
