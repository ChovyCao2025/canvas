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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
    /** 表示已缓存空值的 Redis 字符串哨兵。 */
    static final String NULL_SENTINEL = "__NULL__";

    /** 延迟双删使用独立守护线程，避免阻塞写入调用线程。 */
    private static final ScheduledExecutorService DELAYED_INVALIDATION_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "tiered-cache-delayed-invalidation");
        thread.setDaemon(true);
        return thread;
    });

    /** 缓存实例名称。 */
    @Getter private final String name;
    /** 一级本地缓存最大条目数。 */
    private final int l1MaxSize;
    /** 二级 Redis 缓存键前缀。 */
    private final String l2KeyPrefix;
    /** 二级 Redis 缓存默认过期时间。 */
    private final Duration l2Ttl;
    /** 二级缓存过期时间抖动比例。 */
    private final double l2TtlJitter;
    /** 缓存键结构版本号。 */
    private final int keySchemaVersion;
    /** 空对象占位值过期时间。 */
    private final Duration nullValueTtl;
    /** 空集合或空结果占位值过期时间。 */
    private final Duration emptyValueTtl;
    /** 分布式加载锁过期时间。 */
    private final Duration lockTtl;
    /** 缓存过期前提前刷新窗口。 */
    private final Duration refreshAhead;
    /** 旧值兜底可用时间。 */
    private final Duration staleTtl;
    /** 是否启用热点 key 保护。 */
    private final boolean hotspotProtection;
    /** 缓存穿透保护策略。 */
    private final PenetrationProtectionStrategy penetration;
    /** 缓存击穿保护策略。 */
    private final BreakdownProtectionStrategy breakdown;
    /** 缓存雪崩保护策略。 */
    private final AvalancheProtectionStrategy avalanche;
    /** 缓存 key 合法性校验器。 */
    private final Predicate<K> keyValidator;
    /** 用于拦截不存在 key 的布隆过滤器。 */
    private final CacheBloomFilter<K> bloomFilter;
    /** 数据加载器失败时的处理策略。 */
    private final LoaderFailureStrategy loaderFailure;
    /** Redis 读取失败时的处理策略。 */
    private final RedisFailureStrategy redisReadFailure;
    /** Redis 写入失败时的处理策略。 */
    private final RedisFailureStrategy redisWriteFailure;
    /** Redis 值反序列化失败时的处理策略。 */
    private final DeserializeFailureStrategy deserializeFailure;
    /** L3 数据加载函数。 */
    private final Function<K, V> loader;
    /** 缓存值的 Jackson JavaType。 */
    private final JavaType valueJavaType;
    /** 缓存值序列化与反序列化使用的 ObjectMapper。 */
    private final ObjectMapper objectMapper;
    /** 同步 Redis 操作模板。 */
    private final StringRedisTemplate redis;
    /** 响应式 Redis 操作模板。 */
    @SuppressWarnings("unused")
    private final ReactiveStringRedisTemplate reactiveRedis;
    /** 跨节点本地缓存失效事件发布器。 */
    private final CacheInvalidationPublisher invalidationPublisher;
    /** 一级 Caffeine 本地缓存。 */
    final LoadingCache<K, Optional<V>> l1;
    /** 响应式缓存访问视图。 */
    private final ReactiveTieredCache<K, V> reactiveView;
    /** 正在执行的本地单飞加载任务。 */
    private final Map<K, CompletableFuture<Optional<V>>> inFlightLoads = new ConcurrentHashMap<>();
    /** 本地缓存 key 对应的写入版本号。 */
    private final Map<K, Long> l1Versions = new ConcurrentHashMap<>();
    /** 可在异常或提前刷新场景中兜底的旧值。 */
    private final Map<K, StaleValue<V>> staleValues = new ConcurrentHashMap<>();
    /** 本地记录的一级缓存命中次数。 */
    private final LongAdder localL1Hits = new LongAdder();
    /** 本地记录的一级缓存未命中次数。 */
    private final LongAdder localL1Misses = new LongAdder();
    /** 本地记录的二级缓存命中次数。 */
    private final LongAdder localL2Hits = new LongAdder();
    /** 本地记录的二级缓存未命中次数。 */
    private final LongAdder localL2Misses = new LongAdder();
    /** 本地记录的三级加载次数。 */
    private final LongAdder localL3Loads = new LongAdder();
    /** 本地记录的穿透保护拒绝次数。 */
    private final LongAdder localPenetrationRejects = new LongAdder();
    /** 本地记录的数据加载失败次数。 */
    private final LongAdder localLoaderFailures = new LongAdder();

    /** Micrometer 一级缓存命中计数器。 */
    private Counter l1Hits;
    /** Micrometer 二级缓存命中计数器。 */
    private Counter l2Hits;
    /** Micrometer 三级加载命中计数器。 */
    private Counter l3Hits;
    /** Micrometer 一级缓存未命中计数器。 */
    private Counter l1Misses;
    /** Micrometer 二级缓存未命中计数器。 */
    private Counter l2Misses;
    /** Micrometer 数据加载耗时计时器。 */
    private Timer loadTimer;
    /** Micrometer 穿透保护命中计数器。 */
    private Counter penetrationHits;
    /** Micrometer 热点保护等待计数器。 */
    private Counter hotspotWaits;
    /** Micrometer 数据加载失败计数器。 */
    private Counter loaderFailures;

    /**
     * 构造 TieredCacheImpl 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param name name 方法执行所需的业务参数
     * @param l1MaxSize l1MaxSize 数量、阈值或分页参数
     * @param l1RefreshAfterWrite l1RefreshAfterWrite 方法执行所需的业务参数
     * @param l2KeyPrefix l2KeyPrefix 对应的缓存键、配置键或业务键
     * @param l2Ttl l2Ttl 时间、过期时间或持续时长参数
     * @param l2TtlJitter l2TtlJitter 时间、过期时间或持续时长参数
     * @param keySchemaVersion keySchemaVersion 对应的缓存键、配置键或业务键
     * @param nullValueTtl nullValueTtl 待写入、比较或转换的业务值
     * @param emptyValueTtl emptyValueTtl 待写入、比较或转换的业务值
     * @param lockTtl lockTtl 时间、过期时间或持续时长参数
     * @param refreshAhead refreshAhead 方法执行所需的业务参数
     * @param staleTtl staleTtl 时间、过期时间或持续时长参数
     * @param hotspotProtection hotspotProtection 方法执行所需的业务参数
     * @param penetration penetration 方法执行所需的业务参数
     * @param breakdown breakdown 方法执行所需的业务参数
     * @param avalanche avalanche 方法执行所需的业务参数
     * @param keyValidator keyValidator 对应的缓存键、配置键或业务键
     * @param bloomFilter bloomFilter 方法执行所需的业务参数
     * @param loaderFailure loaderFailure 方法执行所需的业务参数
     * @param redisReadFailure redisReadFailure 方法执行所需的业务参数
     * @param redisWriteFailure redisWriteFailure 方法执行所需的业务参数
     * @param deserializeFailure deserializeFailure 方法执行所需的业务参数
     * @param loader loader 方法执行所需的业务参数
     * @param valueJavaType valueJavaType 待写入、比较或转换的业务值
     * @param objectMapper objectMapper 方法执行所需的业务参数
     * @param redis redis 方法执行所需的业务参数
     * @param reactiveRedis reactiveRedis 方法执行所需的业务参数
     * @param invalidationPublisher invalidationPublisher 方法执行所需的业务参数
     * @param meterRegistry meterRegistry 方法执行所需的业务参数
     */
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

    /**
     * 返回 TieredCacheImpl 的 name 配置值。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * 查询或读取 get 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
    @Override
    public Optional<V> get(K key) {
        if (isRejectedByPenetrationProtection(key)) {
            return Optional.empty();
        }
        // L1 命中且版本仍有效时直接返回，避免进入 Caffeine loader 触发 L2/L3 链路。
        Optional<V> cached = freshL1IfPresent(key);
        if (cached != null) {
            countHit("L1");
            // refreshAhead 只做后台预热，不阻塞本次请求的命中返回。
            maybeRefreshAhead(key);
            return cached;
        }
        countMiss("L1");
        // Caffeine LoadingCache 负责串联 loadFromL2ThenL3，并提供本地 refresh 能力。
        Optional<V> loaded = l1.get(key);
        return loaded == null ? Optional.empty() : loaded;
    }

    /**
     * 查询或读取 get If Present 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
    @Override
    public Optional<V> getIfPresent(K key) {
        if (isRejectedByPenetrationProtection(key)) {
            return Optional.empty();
        }
        // getIfPresent 不触发 L3 loader：只查 L1/L2，适合只想读取已有缓存的调用方。
        Optional<V> cached = freshL1IfPresent(key);
        if (cached != null) {
            countHit("L1");
            maybeRefreshAhead(key);
            return cached;
        }
        countMiss("L1");
        return readL2(key, true).value();
    }

    /**
     * 查询或读取 get 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param loaderOverride loaderOverride 方法执行所需的业务参数
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
    @Override
    public Optional<V> get(K key, Supplier<V> loaderOverride) {
        if (isRejectedByPenetrationProtection(key)) {
            return Optional.empty();
        }
        // loaderOverride 只影响本次未命中加载，不替换缓存实例默认 loader。
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

    /**
     * 写入或记录 put 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param value value 待写入、比较或转换的业务值
     */
    @Override
    public void put(K key, V value) {
        if (writeL2(key, value)) {
            // 先提升版本再写 L1，确保收到旧失效广播的本机不会误删刚写入的新值。
            long version = bumpInvalidationVersion(key);
            putL1(key, Optional.ofNullable(value), version);
            // 保留一份 stale 值，供 loader/Redis 短暂故障时按策略降级返回。
            rememberStale(key, value);
            publishInvalidate(keyToString(key), version);
        }
    }

    /**
     * 查询或读取 get All 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param keys keys 对应的缓存键、配置键或业务键
     * @param batchLoader batchLoader 方法执行所需的业务参数
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
    @Override
    public Map<K, Optional<V>> getAll(Collection<K> keys, Function<Collection<K>, Map<K, V>> batchLoader) {
        Map<K, Optional<V>> result = new LinkedHashMap<>();
        List<K> misses = new java.util.ArrayList<>();
        for (K key : keys) {
            if (isRejectedByPenetrationProtection(key)) {
                result.put(key, Optional.empty());
                continue;
            }
            // 批量读取仍优先利用本机 L1，只有 L1/L2 都未命中的 key 才进入 batchLoader。
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
            // batchLoader 只接收真正 miss 的 key，减少下游数据库或远程服务压力。
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

    /**
     * 删除、清理或失效 invalidate 相关的业务数据。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param key key 对应的缓存键、配置键或业务键
     */
    @Override
    public void invalidate(K key) {
        invalidateLocal(key);
        deleteL2(key);
        long version = bumpInvalidationVersion(key);
        publishInvalidate(keyToString(key), version);
        log.debug("[TIERED_CACHE][{}] invalidate key={}", name, key);
    }

    /**
     * 执行 safe Write 对应的业务逻辑。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param writeAction writeAction 方法执行所需的业务参数
     * @param delayMs delayMs 方法执行所需的业务参数
     */
    @Override
    public void safeWrite(K key, Runnable writeAction, long delayMs) {
        invalidateEverywhere(key);
        writeAction.run();
        if (delayMs > 0) {
            DELAYED_INVALIDATION_EXECUTOR.schedule(() -> invalidateEverywhere(key), delayMs, TimeUnit.MILLISECONDS);
        } else {
            invalidateEverywhere(key);
        }
    }

    private void invalidateEverywhere(K key) {
        invalidateLocal(key);
        deleteL2(key);
        publishInvalidate(keyToString(key), bumpInvalidationVersion(key));
    }

    /**
     * 更新或刷新 refresh 相关的业务数据。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param key key 对应的缓存键、配置键或业务键
     */
    @Override
    public void refresh(K key) {
        invalidateLocal(key);
        deleteL2(key);
        Optional<V> loaded = loadFromL2ThenL3(key, () -> loader.apply(key));
        long version = bumpInvalidationVersion(key);
        putL1(key, loaded, version);
        publishInvalidate(keyToString(key), version);
    }

    /**
     * 执行 as Reactive 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 方法执行后的业务结果
     */
    @Override
    public ReactiveTieredCache<K, V> asReactive() {
        return reactiveView;
    }

    /**
     * 执行 stats 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 方法执行后的业务结果
     */
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

    /**
     * 消费或监听 on Invalidate Broadcast 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param rawKey rawKey 对应的缓存键、配置键或业务键
     */
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

    /**
     * 构建、解析或转换 build Cache Loader 相关的业务数据。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
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

    /**
     * 查询或读取 load From L2 Then L3 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param effectiveLoader effectiveLoader 方法执行所需的业务参数
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
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

    /**
     * 查询或读取 load With Single Flight 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param loaderCall loaderCall 方法执行所需的业务参数
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
    private Optional<V> loadWithSingleFlight(K key, Supplier<Optional<V>> loaderCall) {
        CompletableFuture<Optional<V>> created = new CompletableFuture<>();
        CompletableFuture<Optional<V>> existing = inFlightLoads.putIfAbsent(key, created);
        if (existing != null) {
            // 同一 JVM 内相同 key 只允许一个线程真实加载，其余线程复用同一个结果。
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

    /**
     * 查询或读取 read L2 相关的业务数据。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param cacheInL1 cacheInL1 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
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

    /**
     * 执行 handle L2 Json 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param json json 方法执行所需的业务参数
     * @param cacheInL1 cacheInL1 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    private L2Lookup<V> handleL2Json(K key, String json, boolean cacheInL1) {
        if (json != null) {
            if (NULL_SENTINEL.equals(json)) {
                // 空值哨兵代表“数据库也不存在”，命中后同样回填 L1，防缓存穿透。
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
                    // L2 命中后回填 L1，后续热点访问可避开 Redis。
                    putL1WithCurrentVersion(key, result);
                }
                return new L2Lookup<>(true, result);
            }
        }
        countMiss("L2");
        return new L2Lookup<>(false, Optional.empty());
    }

    private record L2Lookup<T>(
            /** 是否在二级缓存中读取到原始值。 */
            boolean found,
            /** 二级缓存反序列化后的业务值。 */
            Optional<T> value) {}

    /**
     * 查询或读取 load From L3 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param staleValue staleValue 待写入、比较或转换的业务值
     * @param effectiveLoader effectiveLoader 方法执行所需的业务参数
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
    private Optional<V> loadFromL3(K key, Optional<V> staleValue, Supplier<V> effectiveLoader) {
        try {
            countHit("L3");
            // L3 是真实数据源加载，使用 Timer 统计加载耗时用于容量评估。
            V value = loadTimer != null ? loadTimer.record(effectiveLoader::get) : effectiveLoader.get();
            writeL2(key, value);
            rememberStale(key, value);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            increment(loaderFailures);
            localLoaderFailures.increment();
            Optional<V> stale = staleFor(key);
            if (useStaleOnError() && stale.isPresent()) {
                // 读路径优先保护可用性：启用 stale 策略时，加载失败可返回最近一次成功值。
                return stale;
            }
            return switch (loaderFailure) {
                case THROW -> throw asRuntime(e);
                case RETURN_STALE -> staleValue != null ? staleValue : Optional.empty();
                case RETURN_EMPTY -> Optional.empty();
            };
        }
    }

    /**
     * 查询或读取 load From L3 With Lock 相关的业务数据。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param staleValue staleValue 待写入、比较或转换的业务值
     * @param effectiveLoader effectiveLoader 方法执行所需的业务参数
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
    private Optional<V> loadFromL3WithLock(K key, Optional<V> staleValue, Supplier<V> effectiveLoader) {
        String lockKey = "cache:lock:" + l2Key(key);
        String token = UUID.randomUUID().toString();
        boolean locked = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, token, lockTtl));
        if (locked) {
            try {
                // 获取到分布式锁的实例负责回源加载，防止多实例同时击穿到 L3。
                return loadFromL3(key, staleValue, effectiveLoader);
            } finally {
                releaseOwnedLock(lockKey, token);
            }
        }
        // 未拿到锁说明已有实例在加载，短暂等待 L2 回填；超时后再按策略自行回源。
        return waitAndRetryL2(key, staleValue, effectiveLoader);
    }

    /**
     * 执行 release Owned Lock 对应的业务逻辑。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param lockKey lockKey 对应的缓存键、配置键或业务键
     * @param token token 方法执行所需的业务参数
     */
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

    /**
     * 执行 wait And Retry L2 对应的业务逻辑。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param staleValue staleValue 待写入、比较或转换的业务值
     * @param effectiveLoader effectiveLoader 方法执行所需的业务参数
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
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
                    // 等待期间发现 L2 已由持锁实例回填，直接使用该值结束等待。
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

    /**
     * 写入或记录 write L2 相关的业务数据。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param value value 待写入、比较或转换的业务值
     * @return 判断结果，true 表示校验通过或条件成立
     */
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

    /**
     * 删除、清理或失效 delete L2 相关的业务数据。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param key key 对应的缓存键、配置键或业务键
     */
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

    /**
     * 执行 bump Invalidation Version 对应的业务逻辑。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 计算得到的数值结果
     */
    private long bumpInvalidationVersion(K key) {
        try {
            String versionKey = invalidationVersionKey(key);
            Long version = redis.opsForValue().increment(versionKey);
            redis.expire(versionKey, invalidationVersionTtl());
            return version != null ? version : System.currentTimeMillis();
        } catch (Exception e) {
            if (redisWriteFailure == RedisFailureStrategy.FAIL_FAST) {
                throw asRuntime(e);
            }
            log.warn("[TIERED_CACHE][{}] invalidation version bump failed key={}: {}", name, key, e.getMessage());
            return System.currentTimeMillis();
        }
    }

    private Duration invalidationVersionTtl() {
        Duration max = maxDuration(l2Ttl, staleTtl);
        max = maxDuration(max, nullValueTtl);
        max = maxDuration(max, emptyValueTtl);
        max = maxDuration(max, refreshAhead);
        return max.plus(Duration.ofHours(1));
    }

    private static Duration maxDuration(Duration first, Duration second) {
        if (first == null) return second == null ? Duration.ZERO : second;
        if (second == null) return first;
        return first.compareTo(second) >= 0 ? first : second;
    }

    /**
     * 执行 current Invalidation Version 对应的业务逻辑。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 计算得到的数值结果
     */
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

    /**
     * 执行 fresh L1 If Present 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
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

    /**
     * 写入或记录 put L1 With Current Version 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param value value 待写入、比较或转换的业务值
     */
    private void putL1WithCurrentVersion(K key, Optional<V> value) {
        Long version = currentInvalidationVersion(key);
        if (version != null) {
            putL1(key, value, version);
        } else {
            l1.put(key, value);
            l1Versions.remove(key);
        }
    }

    /**
     * 写入或记录 put L1 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param value value 待写入、比较或转换的业务值
     * @param version version 方法执行所需的业务参数
     */
    private void putL1(K key, Optional<V> value, long version) {
        l1.put(key, value);
        l1Versions.put(key, version);
        cleanupDetachedLocalVersions();
    }

    /**
     * 执行 l1 Ttl Nanos 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 计算得到的数值结果
     */
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

    /**
     * 执行 remember Local Version 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     */
    private void rememberLocalVersion(K key) {
        Long version = currentInvalidationVersion(key);
        if (version != null) {
            l1Versions.put(key, version);
        } else {
            l1Versions.remove(key);
        }
    }

    /**
     * 删除、清理或失效 invalidate Local 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     */
    private void invalidateLocal(K key) {
        l1.invalidate(key);
        l1Versions.remove(key);
    }

    /**
     * 删除、清理或失效 cleanup Detached Local Versions 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
    private void cleanupDetachedLocalVersions() {
        if (l1Versions.size() <= l1MaxSize * 2L) {
            return;
        }
        l1Versions.keySet().removeIf(key -> !l1.asMap().containsKey(key));
    }

    /**
     * 发布或发送 publish Invalidate 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param rawKey rawKey 对应的缓存键、配置键或业务键
     * @param version version 方法执行所需的业务参数
     */
    private void publishInvalidate(String rawKey, long version) {
        if (invalidationPublisher != null) {
            invalidationPublisher.publish(new CacheInvalidationEvent(name, rawKey, version));
        }
    }

    /**
     * 删除、清理或失效 invalidate Channel 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    String invalidateChannel() {
        return "tiered-cache:" + name + ":invalidate";
    }

    /**
     * 执行 l2 Key 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 转换或查询得到的字符串结果
     */
    String l2Key(K key) {
        return l2KeyPrefix + "v" + keySchemaVersion + ":" + keyToString(key);
    }

    /**
     * 执行 invalidation Version Key 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 转换或查询得到的字符串结果
     */
    String invalidationVersionKey(K key) {
        return l2KeyPrefix + "v" + keySchemaVersion + ":__invalidate__:" + keyToString(key);
    }

    /**
     * 执行 key To String 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 转换或查询得到的字符串结果
     */
    private String keyToString(K key) {
        return String.valueOf(key);
    }

    /**
     * 执行 actual L2 Ttl 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 方法执行后的业务结果
     */
    private Duration actualL2Ttl() {
        if (l2TtlJitter <= 0 || !useTtlJitter()) {
            return l2Ttl;
        }
        long jitterMillis = (long) (l2Ttl.toMillis()
                * ThreadLocalRandom.current().nextDouble(0, l2TtlJitter));
        return l2Ttl.plus(Duration.ofMillis(jitterMillis));
    }

    /**
     * 执行 serialize 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 转换或查询得到的字符串结果
     */
    private String serialize(V value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Cache serialize failed", e);
        }
    }

    /**
     * 执行 deserialize 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param json json 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    private V deserialize(String json) {
        try {
            return objectMapper.readValue(json, valueJavaType);
        } catch (Exception e) {
            throw new IllegalStateException("Cache deserialize failed", e);
        }
    }

    /**
     * 执行 deserialize With Fallback 对应的业务逻辑。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param json json 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
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

    /**
     * 执行 as Runtime 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param e e 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    private RuntimeException asRuntime(Exception e) {
        return e instanceof RuntimeException runtime ? runtime : new RuntimeException(e);
    }

    /**
     * 注册、调度或初始化 register Metrics 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param registry registry 方法执行所需的业务参数
     */
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

    /**
     * 计算或统计 count Hit 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param level level 方法执行所需的业务参数
     */
    private void countHit(String level) {
        if ("L1".equals(level)) { increment(l1Hits); localL1Hits.increment(); }
        if ("L2".equals(level)) { increment(l2Hits); localL2Hits.increment(); }
        if ("L3".equals(level)) { increment(l3Hits); localL3Loads.increment(); }
    }

    /**
     * 计算或统计 count Miss 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param level level 方法执行所需的业务参数
     */
    private void countMiss(String level) {
        if ("L1".equals(level)) { increment(l1Misses); localL1Misses.increment(); }
        if ("L2".equals(level)) { increment(l2Misses); localL2Misses.increment(); }
    }

    /**
     * 执行 increment 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param counter counter 数量、阈值或分页参数
     */
    private void increment(Counter counter) {
        if (counter != null) {
            counter.increment();
        }
    }

    /**
     * 判断 is Rejected By Penetration Protection 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 判断结果，true 表示校验通过或条件成立
     */
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

    /**
     * 执行 cache Null Values 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean cacheNullValues() {
        return penetration == PenetrationProtectionStrategy.CACHE_NULL_SHORT_TTL
                || penetration == PenetrationProtectionStrategy.CACHE_NULL_AND_BLOOM
                || penetration == PenetrationProtectionStrategy.FULL;
    }

    /**
     * 执行 cache Empty Values 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean cacheEmptyValues() {
        return penetration == PenetrationProtectionStrategy.CACHE_EMPTY_SHORT_TTL
                || penetration == PenetrationProtectionStrategy.FULL;
    }

    /**
     * 执行 use Local Single Flight 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean useLocalSingleFlight() {
        return breakdown == BreakdownProtectionStrategy.LOCAL_SINGLE_FLIGHT
                || breakdown == BreakdownProtectionStrategy.LOCAL_AND_DISTRIBUTED
                || breakdown == BreakdownProtectionStrategy.FULL;
    }

    /**
     * 执行 use Distributed Lock 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean useDistributedLock() {
        return hotspotProtection
                || breakdown == BreakdownProtectionStrategy.DISTRIBUTED_LOCK
                || breakdown == BreakdownProtectionStrategy.LOCAL_AND_DISTRIBUTED
                || breakdown == BreakdownProtectionStrategy.FULL;
    }

    /**
     * 执行 use Stale On Error 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean useStaleOnError() {
        return avalanche == AvalancheProtectionStrategy.STALE_ON_ERROR
                || avalanche == AvalancheProtectionStrategy.FULL
                || breakdown == BreakdownProtectionStrategy.STALE_WHILE_REVALIDATE
                || breakdown == BreakdownProtectionStrategy.FULL;
    }

    /**
     * 执行 use Ttl Jitter 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean useTtlJitter() {
        return avalanche == AvalancheProtectionStrategy.TTL_JITTER
                || avalanche == AvalancheProtectionStrategy.FULL;
    }

    /**
     * 判断 is Empty Value 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private boolean isEmptyValue(V value) {
        return value instanceof Collection<?> collection && collection.isEmpty()
                || value instanceof Map<?, ?> map && map.isEmpty()
                || value instanceof Optional<?> optional && optional.isEmpty();
    }

    /**
     * 执行 remember Stale 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @param value value 待写入、比较或转换的业务值
     */
    private void rememberStale(K key, V value) {
        if (value != null) {
            staleValues.put(key, new StaleValue<>(value, Instant.now()));
            if (bloomFilter != null) {
                bloomFilter.put(key);
            }
        }
    }

    /**
     * 执行 stale For 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
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

    /**
     * 在缓存接近二级过期前触发异步刷新。
     *
     * <p>刷新使用 inFlightLoads 去重，避免热点 key 同时触发多次回源加载。
     *
     * @param key 待检查的缓存 key
     */
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

    private record StaleValue<T>(
            /** 可用于异常兜底或提前刷新的旧值。 */
            T value,
            /** 旧值写入本地兜底区的时间。 */
            Instant createdAt) {}

    private static class ReactiveTieredCacheView<K, V> implements ReactiveTieredCache<K, V> {
        /** 被包装为响应式接口的同步缓存实现。 */
        private final TieredCacheImpl<K, V> delegate;

        /**
         * 构造 ReactiveTieredCacheView 实例，并根据入参初始化依赖、配置或内部状态。
         *
         * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
         *
         * @param delegate delegate 方法执行所需的业务参数
         */
        ReactiveTieredCacheView(TieredCacheImpl<K, V> delegate) {
            this.delegate = delegate;
        }

        /**
         * 查询或读取 get 相关的业务数据。
         *
         * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
         *
         * @param key key 对应的缓存键、配置键或业务键
         * @return 异步执行结果，订阅后产生节点结果或业务响应
         */
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

        /**
         * 查询或读取 get If Present 相关的业务数据。
         *
         * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
         *
         * @param key key 对应的缓存键、配置键或业务键
         * @return 异步执行结果，订阅后产生节点结果或业务响应
         */
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

        /**
         * 写入或记录 put 相关的业务数据。
         *
         * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
         *
         * @param key key 对应的缓存键、配置键或业务键
         * @param value value 待写入、比较或转换的业务值
         * @return 异步执行结果，订阅后产生节点结果或业务响应
         */
        @Override
        public Mono<Void> put(K key, V value) {
            return Mono.fromRunnable(() -> delegate.put(key, value)).subscribeOn(Schedulers.boundedElastic()).then();
        }

        /**
         * 删除、清理或失效 invalidate 相关的业务数据。
         *
         * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
         *
         * @param key key 对应的缓存键、配置键或业务键
         * @return 异步执行结果，订阅后产生节点结果或业务响应
         */
        @Override
        public Mono<Void> invalidate(K key) {
            return Mono.fromRunnable(() -> delegate.invalidate(key)).subscribeOn(Schedulers.boundedElastic()).then();
        }

        /**
         * 更新或刷新 refresh 相关的业务数据。
         *
         * <p>返回值采用 Reactor 异步模型，调用方可继续组合后续处理。
         *
         * @param key key 对应的缓存键、配置键或业务键
         * @return 异步执行结果，订阅后产生节点结果或业务响应
         */
        @Override
        public Mono<Void> refresh(K key) {
            return Mono.fromRunnable(() -> delegate.refresh(key)).subscribeOn(Schedulers.boundedElastic()).then();
        }
    }
}
