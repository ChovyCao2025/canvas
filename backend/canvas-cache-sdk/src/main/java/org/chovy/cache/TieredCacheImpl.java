package org.chovy.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.chovy.cache.strategy.AvalancheProtectionStrategy;
import org.chovy.cache.strategy.BreakdownProtectionStrategy;
import org.chovy.cache.strategy.DeserializeFailureStrategy;
import org.chovy.cache.strategy.LoaderFailureStrategy;
import org.chovy.cache.strategy.PenetrationProtectionStrategy;
import org.chovy.cache.strategy.RedisFailureStrategy;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.concurrent.atomic.LongAdder;

/**
 * 分层缓存默认实现，串联本地缓存、Redis 缓存、数据加载器和保护策略。
 *
 * <p>核心读取链路按 L1 → L2 → L3 顺序命中，命中下层后会回填上层以提升后续访问性能。
 * <p>该实现还负责统计缓存指标、处理穿透/击穿/雪崩保护，以及在写路径上执行一致性策略。
 */
@Slf4j
public class TieredCacheImpl<K, V> implements TieredCache<K, V> {
    static final String NULL_SENTINEL = "__NULL__";

    @Getter private final String name;
    private final int l1MaxSize;
    private final String l2KeyPrefix;
    private final Duration l2Ttl;
    private final double l2TtlJitter;
    private final int keySchemaVersion;
    private final Duration nullValueTtl;
    private final Duration emptyValueTtl;
    private final Duration lockTtl;
    private final Duration refreshAhead;
    private final Duration staleTtl;
    private final boolean hotspotProtection;
    private final PenetrationProtectionStrategy penetration;
    private final BreakdownProtectionStrategy breakdown;
    private final AvalancheProtectionStrategy avalanche;
    private final Predicate<K> keyValidator;
    private final CacheBloomFilter<K> bloomFilter;
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
    private final CacheInvalidationPublisher invalidationPublisher;
    final LoadingCache<K, Optional<V>> l1;
    private final ReactiveTieredCache<K, V> reactiveView;
    private final Map<K, CompletableFuture<Optional<V>>> inFlightLoads = new ConcurrentHashMap<>();
    private final Map<K, Long> l1Versions = new ConcurrentHashMap<>();
    private final Map<K, StaleValue<V>> staleValues = new ConcurrentHashMap<>();
    private final LongAdder localL1Hits = new LongAdder();
    private final LongAdder localL1Misses = new LongAdder();
    private final LongAdder localL2Hits = new LongAdder();
    private final LongAdder localL2Misses = new LongAdder();
    private final LongAdder localL3Loads = new LongAdder();
    private final LongAdder localPenetrationRejects = new LongAdder();
    private final LongAdder localLoaderFailures = new LongAdder();

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
                    Duration emptyValueTtl,
                    Duration lockTtl,
                    Duration refreshAhead,
                    Duration staleTtl,
                    boolean hotspotProtection,
                    PenetrationProtectionStrategy penetration,
                    BreakdownProtectionStrategy breakdown,
                    AvalancheProtectionStrategy avalanche,
                    Predicate<K> keyValidator,
                    CacheBloomFilter<K> bloomFilter,
                    LoaderFailureStrategy loaderFailure,
                    RedisFailureStrategy redisReadFailure,
                    RedisFailureStrategy redisWriteFailure,
                    DeserializeFailureStrategy deserializeFailure,
                    Function<K, V> loader,
                    JavaType valueJavaType,
                    ObjectMapper objectMapper,
                    StringRedisTemplate redis,
                    ReactiveStringRedisTemplate reactiveRedis,
                    CacheInvalidationPublisher invalidationPublisher,
                    MeterRegistry meterRegistry) {
        this.name = name;
        this.l1MaxSize = l1MaxSize;
        this.l2KeyPrefix = l2KeyPrefix;
        this.l2Ttl = l2Ttl;
        this.l2TtlJitter = l2TtlJitter;
        this.keySchemaVersion = keySchemaVersion;
        this.nullValueTtl = nullValueTtl;
        this.emptyValueTtl = emptyValueTtl;
        this.lockTtl = lockTtl;
        this.refreshAhead = refreshAhead;
        this.staleTtl = staleTtl;
        this.hotspotProtection = hotspotProtection;
        this.penetration = penetration;
        this.breakdown = breakdown;
        this.avalanche = avalanche;
        this.keyValidator = keyValidator;
        this.bloomFilter = bloomFilter;
        this.loaderFailure = loaderFailure;
        this.redisReadFailure = redisReadFailure;
        this.redisWriteFailure = redisWriteFailure;
        this.deserializeFailure = deserializeFailure;
        this.loader = loader;
        this.valueJavaType = valueJavaType;
        this.objectMapper = objectMapper;
        this.redis = redis;
        this.reactiveRedis = reactiveRedis;
        this.invalidationPublisher = invalidationPublisher;
        this.l1 = Caffeine.newBuilder()
                .maximumSize(l1MaxSize)
                .expireAfter(new Expiry<K, Optional<V>>() {
                    @Override
                    public long expireAfterCreate(K key, Optional<V> value, long currentTime) {
                        return l1TtlNanos(value);
                    }

                    @Override
                    public long expireAfterUpdate(K key, Optional<V> value, long currentTime, long currentDuration) {
                        return l1TtlNanos(value);
                    }

                    @Override
                    public long expireAfterRead(K key, Optional<V> value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
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
        if (isRejectedByPenetrationProtection(key)) {
            return Optional.empty();
        }
        Optional<V> cached = freshL1IfPresent(key);
        if (cached != null) {
            countHit("L1");
            maybeRefreshAhead(key);
            return cached;
        }
        countMiss("L1");
        Optional<V> loaded = l1.get(key);
        return loaded == null ? Optional.empty() : loaded;
    }

    @Override
    public Optional<V> getIfPresent(K key) {
        if (isRejectedByPenetrationProtection(key)) {
            return Optional.empty();
        }
        Optional<V> cached = freshL1IfPresent(key);
        if (cached != null) {
            countHit("L1");
            maybeRefreshAhead(key);
            return cached;
        }
        countMiss("L1");
        return readL2(key, true).value();
    }

    @Override
    public Optional<V> get(K key, Supplier<V> loaderOverride) {
        if (isRejectedByPenetrationProtection(key)) {
            return Optional.empty();
        }
        Optional<V> cached = freshL1IfPresent(key);
        if (cached != null) {
            countHit("L1");
            maybeRefreshAhead(key);
            return cached;
        }
        countMiss("L1");
        Optional<V> loaded = loadFromL2ThenL3(key, loaderOverride);
        putL1WithCurrentVersion(key, loaded);
        return loaded;
    }

    @Override
    public void put(K key, V value) {
        if (writeL2(key, value)) {
            long version = bumpInvalidationVersion(key);
            putL1(key, Optional.ofNullable(value), version);
            rememberStale(key, value);
            publishInvalidate(keyToString(key), version);
        }
    }

    @Override
    public Map<K, Optional<V>> getAll(Collection<K> keys, Function<Collection<K>, Map<K, V>> batchLoader) {
        Map<K, Optional<V>> result = new LinkedHashMap<>();
        List<K> misses = new java.util.ArrayList<>();
        for (K key : keys) {
            if (isRejectedByPenetrationProtection(key)) {
                result.put(key, Optional.empty());
                continue;
            }
            Optional<V> cached = freshL1IfPresent(key);
            if (cached != null) {
                countHit("L1");
                result.put(key, cached);
                continue;
            }
            countMiss("L1");
            L2Lookup<V> l2Lookup = readL2(key, true);
            if (l2Lookup.found()) {
                result.put(key, l2Lookup.value());
            } else {
                misses.add(key);
            }
        }
        if (!misses.isEmpty()) {
            Map<K, V> loaded = batchLoader.apply(List.copyOf(misses));
            for (K key : misses) {
                V value = loaded.get(key);
                writeL2(key, value);
                Optional<V> optional = Optional.ofNullable(value);
                putL1WithCurrentVersion(key, optional);
                rememberStale(key, value);
                result.put(key, optional);
            }
        }
        return result;
    }

    @Override
    public void invalidate(K key) {
        invalidateLocal(key);
        deleteL2(key);
        long version = bumpInvalidationVersion(key);
        publishInvalidate(keyToString(key), version);
        log.debug("[TIERED_CACHE][{}] invalidate key={}", name, key);
    }

    @Override
    public void safeWrite(K key, Runnable writeAction, long delayMs) {
        invalidateLocal(key);
        deleteL2(key);
        publishInvalidate(keyToString(key), bumpInvalidationVersion(key));
        try {
            writeAction.run();
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        invalidateLocal(key);
        deleteL2(key);
        publishInvalidate(keyToString(key), bumpInvalidationVersion(key));
    }

    @Override
    public void refresh(K key) {
        invalidateLocal(key);
        deleteL2(key);
        Optional<V> loaded = loadFromL2ThenL3(key, () -> loader.apply(key));
        long version = bumpInvalidationVersion(key);
        putL1(key, loaded, version);
        publishInvalidate(keyToString(key), version);
    }

    @Override
    public ReactiveTieredCache<K, V> asReactive() {
        return reactiveView;
    }

    @Override
    public TieredCacheStats stats() {
        return new TieredCacheStats(
                name,
                l1.estimatedSize(),
                localL1Hits.sum(),
                localL1Misses.sum(),
                localL2Hits.sum(),
                localL2Misses.sum(),
                localL3Loads.sum(),
                localPenetrationRejects.sum(),
                localLoaderFailures.sum());
    }

    void onInvalidateBroadcast(String rawKey) {
        l1.asMap().keySet().removeIf(key -> {
            boolean matches = keyToString(key).equals(rawKey);
            if (matches) {
                l1Versions.remove(key);
            }
            return matches;
        });
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
                            rememberLocalVersion(key);
                            return Optional.empty();
                        }
                        Optional<V> value = Optional.ofNullable(deserialize(json));
                        rememberLocalVersion(key);
                        return value;
                    }
                } catch (Exception e) {
                    if (redisReadFailure == RedisFailureStrategy.FAIL_FAST) {
                        throw asRuntime(e);
                    }
                    log.warn("[TIERED_CACHE][{}] Redis read failed on refresh: {}", name, e.getMessage());
                }
                Optional<V> value = loadFromL3(key, oldValue, () -> loader.apply(key));
                rememberLocalVersion(key);
                return value;
            }
        };
    }

    private Optional<V> loadFromL2ThenL3(K key, Supplier<V> effectiveLoader) {
        Optional<V> loaded;
        L2Lookup<V> l2Lookup = readL2(key, false);
        if (l2Lookup.found()) {
            loaded = l2Lookup.value();
        } else {
            Supplier<Optional<V>> l3Call = () -> useDistributedLock()
                    ? loadFromL3WithLock(key, null, effectiveLoader)
                    : loadFromL3(key, null, effectiveLoader);
            loaded = useLocalSingleFlight() ? loadWithSingleFlight(key, l3Call) : l3Call.get();
        }
        rememberLocalVersion(key);
        return loaded;
    }

    private Optional<V> loadWithSingleFlight(K key, Supplier<Optional<V>> loaderCall) {
        CompletableFuture<Optional<V>> created = new CompletableFuture<>();
        CompletableFuture<Optional<V>> existing = inFlightLoads.putIfAbsent(key, created);
        if (existing != null) {
            return existing.join();
        }
        try {
            Optional<V> result = loaderCall.get();
            created.complete(result);
            return result;
        } catch (RuntimeException e) {
            created.completeExceptionally(e);
            throw e;
        } finally {
            inFlightLoads.remove(key, created);
        }
    }

    private L2Lookup<V> readL2(K key, boolean cacheInL1) {
        try {
            String json = redis.opsForValue().get(l2Key(key));
            return handleL2Json(key, json, cacheInL1);
        } catch (Exception e) {
            if (redisReadFailure == RedisFailureStrategy.FAIL_FAST) {
                throw asRuntime(e);
            }
            log.warn("[TIERED_CACHE][{}] Redis read failed, fallthrough to L3: {}", name, e.getMessage());
            return new L2Lookup<>(false, Optional.empty());
        }
    }

    private L2Lookup<V> handleL2Json(K key, String json, boolean cacheInL1) {
        if (json != null) {
            if (NULL_SENTINEL.equals(json)) {
                countHit("L2");
                increment(penetrationHits);
                if (cacheInL1) {
                    putL1WithCurrentVersion(key, Optional.empty());
                }
                return new L2Lookup<>(true, Optional.empty());
            }
            V value = deserializeWithFallback(key, json);
            if (value != null) {
                countHit("L2");
                Optional<V> result = Optional.of(value);
                if (cacheInL1) {
                    putL1WithCurrentVersion(key, result);
                }
                return new L2Lookup<>(true, result);
            }
        }
        countMiss("L2");
        return new L2Lookup<>(false, Optional.empty());
    }

    private record L2Lookup<T>(boolean found, Optional<T> value) {}

    private Optional<V> loadFromL3(K key, Optional<V> staleValue, Supplier<V> effectiveLoader) {
        try {
            countHit("L3");
            V value = loadTimer != null ? loadTimer.record(effectiveLoader::get) : effectiveLoader.get();
            writeL2(key, value);
            rememberStale(key, value);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            increment(loaderFailures);
            localLoaderFailures.increment();
            Optional<V> stale = staleFor(key);
            if (useStaleOnError() && stale.isPresent()) {
                return stale;
            }
            return switch (loaderFailure) {
                case THROW -> throw asRuntime(e);
                case RETURN_STALE -> staleValue != null ? staleValue : Optional.empty();
                case RETURN_EMPTY -> Optional.empty();
            };
        }
    }

    private Optional<V> loadFromL3WithLock(K key, Optional<V> staleValue, Supplier<V> effectiveLoader) {
        String lockKey = "cache:lock:" + l2Key(key);
        String token = UUID.randomUUID().toString();
        boolean locked = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, token, lockTtl));
        if (locked) {
            try {
                return loadFromL3(key, staleValue, effectiveLoader);
            } finally {
                releaseOwnedLock(lockKey, token);
            }
        }
        return waitAndRetryL2(key, staleValue, effectiveLoader);
    }

    private void releaseOwnedLock(String lockKey, String token) {
        String script = """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                end
                return 0
                """;
        try {
            redis.execute(RedisScript.of(script, Long.class), List.of(lockKey), token);
        } catch (Exception e) {
            log.warn("[TIERED_CACHE][{}] Redis lock release failed: {}", name, e.getMessage());
        }
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
                if (cacheNullValues()) {
                    redis.opsForValue().set(l2Key(key), NULL_SENTINEL, nullValueTtl);
                }
            } else if (isEmptyValue(value) && cacheEmptyValues()) {
                redis.opsForValue().set(l2Key(key), serialize(value), emptyValueTtl);
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

    private long bumpInvalidationVersion(K key) {
        try {
            Long version = redis.opsForValue().increment(invalidationVersionKey(key));
            return version != null ? version : System.currentTimeMillis();
        } catch (Exception e) {
            if (redisWriteFailure == RedisFailureStrategy.FAIL_FAST) {
                throw asRuntime(e);
            }
            log.warn("[TIERED_CACHE][{}] invalidation version bump failed key={}: {}", name, key, e.getMessage());
            return System.currentTimeMillis();
        }
    }

    private Long currentInvalidationVersion(K key) {
        try {
            String version = redis.opsForValue().get(invalidationVersionKey(key));
            return version == null || version.isBlank() ? 0L : Long.parseLong(version);
        } catch (NumberFormatException e) {
            log.warn("[TIERED_CACHE][{}] invalidation version is invalid key={}: {}", name, key, e.getMessage());
            return null;
        } catch (Exception e) {
            if (redisReadFailure == RedisFailureStrategy.FAIL_FAST) {
                throw asRuntime(e);
            }
            log.warn("[TIERED_CACHE][{}] invalidation version read failed key={}: {}", name, key, e.getMessage());
            return null;
        }
    }

    private Optional<V> freshL1IfPresent(K key) {
        Optional<V> cached = l1.getIfPresent(key);
        if (cached == null) {
            return null;
        }
        Long localVersion = l1Versions.get(key);
        Long currentVersion = currentInvalidationVersion(key);
        if (localVersion != null && currentVersion != null && localVersion >= currentVersion) {
            return cached;
        }
        invalidateLocal(key);
        return null;
    }

    private void putL1WithCurrentVersion(K key, Optional<V> value) {
        Long version = currentInvalidationVersion(key);
        if (version != null) {
            putL1(key, value, version);
        } else {
            l1.put(key, value);
            l1Versions.remove(key);
        }
    }

    private void putL1(K key, Optional<V> value, long version) {
        l1.put(key, value);
        l1Versions.put(key, version);
        cleanupDetachedLocalVersions();
    }

    private long l1TtlNanos(Optional<V> value) {
        Duration ttl;
        if (value == null || value.isEmpty()) {
            ttl = nullValueTtl;
        } else if (isEmptyValue(value.get())) {
            ttl = emptyValueTtl;
        } else {
            ttl = l2Ttl;
        }
        return Math.max(1L, ttl.toNanos());
    }

    private void rememberLocalVersion(K key) {
        Long version = currentInvalidationVersion(key);
        if (version != null) {
            l1Versions.put(key, version);
        } else {
            l1Versions.remove(key);
        }
    }

    private void invalidateLocal(K key) {
        l1.invalidate(key);
        l1Versions.remove(key);
    }

    private void cleanupDetachedLocalVersions() {
        if (l1Versions.size() <= l1MaxSize * 2L) {
            return;
        }
        l1Versions.keySet().removeIf(key -> !l1.asMap().containsKey(key));
    }

    private void publishInvalidate(String rawKey, long version) {
        if (invalidationPublisher != null) {
            invalidationPublisher.publish(new CacheInvalidationEvent(name, rawKey, version));
        }
    }

    String invalidateChannel() {
        return "tiered-cache:" + name + ":invalidate";
    }

    String l2Key(K key) {
        return l2KeyPrefix + "v" + keySchemaVersion + ":" + keyToString(key);
    }

    String invalidationVersionKey(K key) {
        return l2KeyPrefix + "v" + keySchemaVersion + ":__invalidate__:" + keyToString(key);
    }

    private String keyToString(K key) {
        return String.valueOf(key);
    }

    private Duration actualL2Ttl() {
        if (l2TtlJitter <= 0 || !useTtlJitter()) {
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
        if ("L1".equals(level)) { increment(l1Hits); localL1Hits.increment(); }
        if ("L2".equals(level)) { increment(l2Hits); localL2Hits.increment(); }
        if ("L3".equals(level)) { increment(l3Hits); localL3Loads.increment(); }
    }

    private void countMiss(String level) {
        if ("L1".equals(level)) { increment(l1Misses); localL1Misses.increment(); }
        if ("L2".equals(level)) { increment(l2Misses); localL2Misses.increment(); }
    }

    private void increment(Counter counter) {
        if (counter != null) {
            counter.increment();
        }
    }

    private boolean isRejectedByPenetrationProtection(K key) {
        boolean useValidator = penetration == PenetrationProtectionStrategy.KEY_VALIDATOR
                || penetration == PenetrationProtectionStrategy.FULL;
        if (useValidator && keyValidator != null && !keyValidator.test(key)) {
            localPenetrationRejects.increment();
            return true;
        }
        boolean useBloom = penetration == PenetrationProtectionStrategy.BLOOM_FILTER
                || penetration == PenetrationProtectionStrategy.CACHE_NULL_AND_BLOOM
                || penetration == PenetrationProtectionStrategy.FULL;
        if (useBloom && bloomFilter != null && !bloomFilter.mightContain(key)) {
            localPenetrationRejects.increment();
            return true;
        }
        return false;
    }

    private boolean cacheNullValues() {
        return penetration == PenetrationProtectionStrategy.CACHE_NULL_SHORT_TTL
                || penetration == PenetrationProtectionStrategy.CACHE_NULL_AND_BLOOM
                || penetration == PenetrationProtectionStrategy.FULL;
    }

    private boolean cacheEmptyValues() {
        return penetration == PenetrationProtectionStrategy.CACHE_EMPTY_SHORT_TTL
                || penetration == PenetrationProtectionStrategy.FULL;
    }

    private boolean useLocalSingleFlight() {
        return breakdown == BreakdownProtectionStrategy.LOCAL_SINGLE_FLIGHT
                || breakdown == BreakdownProtectionStrategy.LOCAL_AND_DISTRIBUTED
                || breakdown == BreakdownProtectionStrategy.FULL;
    }

    private boolean useDistributedLock() {
        return hotspotProtection
                || breakdown == BreakdownProtectionStrategy.DISTRIBUTED_LOCK
                || breakdown == BreakdownProtectionStrategy.LOCAL_AND_DISTRIBUTED
                || breakdown == BreakdownProtectionStrategy.FULL;
    }

    private boolean useStaleOnError() {
        return avalanche == AvalancheProtectionStrategy.STALE_ON_ERROR
                || avalanche == AvalancheProtectionStrategy.FULL
                || breakdown == BreakdownProtectionStrategy.STALE_WHILE_REVALIDATE
                || breakdown == BreakdownProtectionStrategy.FULL;
    }

    private boolean useTtlJitter() {
        return avalanche == AvalancheProtectionStrategy.TTL_JITTER
                || avalanche == AvalancheProtectionStrategy.FULL;
    }

    private boolean isEmptyValue(V value) {
        return value instanceof Collection<?> collection && collection.isEmpty()
                || value instanceof Map<?, ?> map && map.isEmpty()
                || value instanceof Optional<?> optional && optional.isEmpty();
    }

    private void rememberStale(K key, V value) {
        if (value != null) {
            staleValues.put(key, new StaleValue<>(value, Instant.now()));
            if (bloomFilter != null) {
                bloomFilter.put(key);
            }
        }
    }

    private Optional<V> staleFor(K key) {
        StaleValue<V> stale = staleValues.get(key);
        if (stale == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(stale.createdAt().plus(staleTtl))) {
            staleValues.remove(key, stale);
            return Optional.empty();
        }
        return Optional.of(stale.value());
    }

    private void maybeRefreshAhead(K key) {
        if (refreshAhead.isZero() || refreshAhead.compareTo(l2Ttl) >= 0) {
            return;
        }
        StaleValue<V> current = staleValues.get(key);
        if (current == null) {
            return;
        }
        Instant refreshAt = current.createdAt().plus(l2Ttl).minus(refreshAhead);
        if (Instant.now().isBefore(refreshAt)) {
            return;
        }
        CompletableFuture<Optional<V>> marker = new CompletableFuture<>();
        if (inFlightLoads.putIfAbsent(key, marker) != null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                L2Lookup<V> l2Lookup = readL2(key, false);
                Optional<V> refreshed = l2Lookup.found()
                        ? l2Lookup.value()
                        : (useDistributedLock()
                        ? loadFromL3WithLock(key, staleFor(key), () -> loader.apply(key))
                        : loadFromL3(key, staleFor(key), () -> loader.apply(key)));
                putL1WithCurrentVersion(key, refreshed);
                marker.complete(refreshed);
            } catch (RuntimeException e) {
                marker.completeExceptionally(e);
                log.warn("[TIERED_CACHE][{}] refreshAhead failed key={}: {}", name, key, e.getMessage());
            } finally {
                inFlightLoads.remove(key, marker);
            }
        });
    }

    private record StaleValue<T>(T value, Instant createdAt) {}

    private static class ReactiveTieredCacheView<K, V> implements ReactiveTieredCache<K, V> {
        private final TieredCacheImpl<K, V> delegate;

        ReactiveTieredCacheView(TieredCacheImpl<K, V> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Mono<Optional<V>> get(K key) {
            Optional<V> cached = delegate.freshL1IfPresent(key);
            if (cached != null) {
                delegate.countHit("L1");
                return Mono.just(cached);
            }
            delegate.countMiss("L1");
            if (delegate.reactiveRedis == null) {
                return Mono.fromCallable(() -> delegate.get(key)).subscribeOn(Schedulers.boundedElastic());
            }
            Mono<String> redisGet = delegate.reactiveRedis.opsForValue().get(delegate.l2Key(key));
            if (redisGet == null) {
                return Mono.fromCallable(() -> delegate.get(key)).subscribeOn(Schedulers.boundedElastic());
            }
            return redisGet
                    .flatMap(json -> {
                        L2Lookup<V> l2Value = delegate.handleL2Json(key, json, true);
                        return Mono.just(l2Value.value());
                    })
                    .switchIfEmpty(Mono.fromCallable(() -> delegate.hotspotProtection
                                    ? delegate.loadFromL3WithLock(key, null, () -> delegate.loader.apply(key))
                                    : delegate.loadFromL3(key, null, () -> delegate.loader.apply(key)))
                            .subscribeOn(Schedulers.boundedElastic()));
        }

        @Override
        public Mono<Optional<V>> getIfPresent(K key) {
            Optional<V> cached = delegate.freshL1IfPresent(key);
            if (cached != null) {
                delegate.countHit("L1");
                return Mono.just(cached);
            }
            delegate.countMiss("L1");
            if (delegate.reactiveRedis == null) {
                return Mono.fromCallable(() -> delegate.getIfPresent(key)).subscribeOn(Schedulers.boundedElastic());
            }
            Mono<String> redisGet = delegate.reactiveRedis.opsForValue().get(delegate.l2Key(key));
            if (redisGet == null) {
                return Mono.fromCallable(() -> delegate.getIfPresent(key)).subscribeOn(Schedulers.boundedElastic());
            }
            return redisGet
                    .map(json -> delegate.handleL2Json(key, json, true).value())
                    .defaultIfEmpty(Optional.empty());
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
