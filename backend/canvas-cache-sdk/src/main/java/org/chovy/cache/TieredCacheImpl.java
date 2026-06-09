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
     * 创建完整的分层缓存实现。
     *
     * <p>构造阶段会初始化 Caffeine L1、Redis L2/L3 加载链路、失效版本表、响应式视图和可选 Micrometer 指标。
     *
     * @param name 缓存实例名称
     * @param l1MaxSize L1 本地缓存最大条目数
     * @param l1RefreshAfterWrite L1 写入后的刷新间隔
     * @param l2KeyPrefix L2 Redis key 前缀
     * @param l2Ttl L2 正常值 TTL
     * @param l2TtlJitter L2 TTL 抖动比例
     * @param keySchemaVersion 缓存 key 结构版本
     * @param nullValueTtl null 占位 TTL
     * @param emptyValueTtl 空结果 TTL
     * @param lockTtl Redis 分布式加载锁 TTL
     * @param refreshAhead 提前刷新窗口
     * @param staleTtl 旧值兜底可用时间
     * @param hotspotProtection 是否启用热点 key 保护
     * @param penetration 穿透保护策略
     * @param breakdown 击穿保护策略
     * @param avalanche 雪崩保护策略
     * @param keyValidator 业务 key 校验器
     * @param bloomFilter 布隆过滤器
     * @param loaderFailure L3 加载失败策略
     * @param redisReadFailure Redis 读取失败策略
     * @param redisWriteFailure Redis 写入失败策略
     * @param deserializeFailure L2 反序列化失败策略
     * @param loader 默认 L3 加载器
     * @param valueJavaType 缓存值反序列化类型
     * @param objectMapper JSON 序列化组件
     * @param redis 同步 Redis 模板
     * @param reactiveRedis 响应式 Redis 模板
     * @param invalidationPublisher 跨节点本地缓存失效发布器
     * @param meterRegistry 指标注册表，为 null 时不注册 Micrometer 指标
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
                    /**
                     * 为新写入 L1 的值计算过期时间。
                     *
                     * <p>null/空结果使用短 TTL，正常业务值使用标准 L1 TTL，避免穿透占位长期占用本地缓存。
                     */
                    public long expireAfterCreate(K key, Optional<V> value, long currentTime) {
                        return l1TtlNanos(value);
                    }

                    @Override
                    /**
                     * 为 L1 已存在 key 的新值重新计算过期时间。
                     *
                     * <p>更新后的空值占位仍保持短 TTL，正常值继续沿用标准 L1 TTL。
                     */
                    public long expireAfterUpdate(K key, Optional<V> value, long currentTime, long currentDuration) {
                        return l1TtlNanos(value);
                    }

                    @Override
                    /**
                     * 读取 L1 时保持当前剩余过期时间不变。
                     *
                     * <p>缓存命中不会被延长 TTL，避免热点 key 在本地无限续期后绕过跨节点失效版本检查。
                     */
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
     * 返回缓存实例名称。
     *
     * <p>该名称用于指标标签、失效事件路由和 Redis key 命名。
     *
     * @return 缓存实例名称
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * 按 L1、L2、L3 顺序读取缓存值。
     *
     * <p>先做穿透保护和本地版本检查，L1 未命中后由 Caffeine loader 串联 Redis 和 L3 加载器。
     *
     * @param key 业务缓存 key
     * @return 缓存值；业务不存在或空值占位命中时为空
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
     * 只读取 L1/L2 中已经存在的缓存值。
     *
     * <p>该方法不会触发 L3 加载器，适合调用方只想探测缓存状态的场景。
     *
     * @param key 业务缓存 key
     * @return 已缓存的值；缓存未命中或空值占位命中时为空
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
     * 使用本次调用提供的加载器读取缓存值。
     *
     * <p>L1/L2 命中时不会执行 {@code loaderOverride}；未命中时仍会应用击穿保护、Redis 回填和失败策略。
     *
     * @param key 业务缓存 key
     * @param loaderOverride 本次 L3 回源加载函数
     * @return 缓存值；业务不存在或空值占位命中时为空
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
     * 主动写入单个缓存值到 L2 和 L1。
     *
     * <p>Redis 写入成功后提升失效版本、更新 L1、保存 stale 兜底值，并广播跨节点本地缓存失效事件。
     *
     * @param key 业务缓存 key
     * @param value 待缓存的业务值
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
     * 批量读取缓存并对真正未命中的 key 批量回源。
     *
     * <p>每个 key 都先走穿透保护、L1 和 L2；只有未命中的 key 会交给 {@code batchLoader} 并按策略写回两级缓存。
     *
     * @param keys 需要读取的业务缓存 key 集合
     * @param batchLoader 未命中 key 的批量加载函数
     * @return 每个 key 对应的缓存值
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
            Map<K, V> loaded;
            try {
                loaded = batchLoader.apply(List.copyOf(misses));
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (Exception e) {
                for (K key : misses) {
                    Optional<V> optional = handleLoaderFailure(key, staleFor(key), e);
                    putL1WithCurrentVersion(key, optional);
                    result.put(key, optional);
                }
                return result;
            }
            if (loaded == null) {
                loaded = Map.of();
            }
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
     * 失效单个 key 的 L1 和 L2 缓存。
     *
     * <p>先清理本节点 L1，再删除 Redis L2，并提升版本后广播失效，通知其他节点清理本地缓存。
     *
     * @param key 需要失效的业务缓存 key
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
     * 执行业务写入并做延迟双删。
     *
     * <p>写入动作前先全局失效缓存，写入完成后立即或延迟再次失效，降低并发读写导致脏缓存回填的概率。
     *
     * @param key 需要保护一致性的业务缓存 key
     * @param writeAction 实际写入数据库或下游系统的动作
     * @param delayMs 第二次失效前等待的毫秒数
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

    /**
     * 同时清理本地 L1、Redis L2 并广播版本化失效事件。
     *
     * @param key 需要全局失效的业务缓存 key
     */
    private void invalidateEverywhere(K key) {
        invalidateLocal(key);
        deleteL2(key);
        publishInvalidate(keyToString(key), bumpInvalidationVersion(key));
    }

    /**
     * 强制重新加载并回填缓存。
     *
     * <p>先删除旧的 L1/L2，再通过 L3 加载器读取新值，最后写入 L1 并广播版本化失效事件。
     *
     * @param key 需要刷新的业务缓存 key
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
     * 返回当前缓存的响应式视图。
     *
     * <p>响应式视图复用同步缓存的 L1 状态、Redis key 规则、击穿保护和失败策略。
     *
     * @return 响应式缓存接口
     */
    @Override
    public ReactiveTieredCache<K, V> asReactive() {
        return reactiveView;
    }

    /**
     * 生成当前缓存的指标快照。
     *
     * <p>快照读取 L1 估算大小和本地计数器，不会重置统计数据。
     *
     * @return 缓存运行指标快照
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
     * 处理其他节点广播过来的本地缓存失效事件。
     *
     * <p>该方法只按原始业务 key 清理本节点 L1 和本地版本，不删除 Redis，也不再次广播事件。
     *
     * @param rawKey 需要失效的原始业务 key 字符串
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
     * 构建 Caffeine LoadingCache 使用的加载器。
     *
     * <p>load 串联 L2 Redis 和 L3 加载器；reload 优先重新读取 L2，只有 L2 未命中或按策略降级时才回源 L3。
     *
     * @return L1 本地缓存加载器
     */
    private CacheLoader<K, Optional<V>> buildCacheLoader() {
        return new CacheLoader<>() {
            /**
             * L1 未命中时加载缓存值。
             *
             * <p>该方法先读取 L2，未命中后按击穿保护策略访问 L3，并把结果交由调用方写回 L1。
             */
            @Override
            public Optional<V> load(K key) {
                return loadFromL2ThenL3(key, () -> loader.apply(key));
            }

            /**
             * L1 refresh 时重新加载缓存值。
             *
             * <p>刷新优先相信 L2 的最新值；Redis 读取失败时按 redisReadFailure 决定快速失败或降级回源。
             */
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
                // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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
     * 从 L2 读取，未命中时回源 L3。
     *
     * <p>该方法是 get 路径的核心分层链路，负责命中计数、击穿保护选择和本地版本记录。
     *
     * @param key 业务缓存 key
     * @param effectiveLoader 当前调用使用的 L3 加载函数
     * @return 读取或加载得到的业务值
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
     * 在当前 JVM 内合并相同 key 的并发加载。
     *
     * <p>第一个线程负责真实执行 loader，其余线程等待同一个 CompletableFuture，避免本机击穿。
     *
     * @param key 业务缓存 key
     * @param loaderCall 实际加载动作
     * @return 合并后的加载结果
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            created.completeExceptionally(e);
            throw e;
        } finally {
            inFlightLoads.remove(key, created);
        }
    }

    /**
     * 读取 L2 Redis 缓存。
     *
     * <p>Redis 读取失败时按 redisReadFailure 选择快速失败或返回未命中，后者会继续进入 L3。
     *
     * @param key 业务缓存 key
     * @param cacheInL1 L2 命中后是否回填 L1
     * @return L2 查找结果，区分“未命中”和“命中空值占位”
     */
    private L2Lookup<V> readL2(K key, boolean cacheInL1) {
        try {
            String json = redis.opsForValue().get(l2Key(key));
            return handleL2Json(key, json, cacheInL1);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            if (redisReadFailure == RedisFailureStrategy.FAIL_FAST) {
                throw asRuntime(e);
            }
            log.warn("[TIERED_CACHE][{}] Redis read failed, fallthrough to L3: {}", name, e.getMessage());
            return new L2Lookup<>(false, Optional.empty());
        }
    }

    /**
     * 解析 L2 Redis 返回的 JSON 或空值哨兵。
     *
     * <p>命中正常值时反序列化并可回填 L1；命中 null 哨兵时返回空 Optional 并计入穿透命中。
     *
     * @param key 业务缓存 key
     * @param json Redis 中读取到的原始字符串
     * @param cacheInL1 是否将解析结果回填 L1
     * @return L2 查找结果
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

    /**
     * L2Lookup record.
     * @param found 是否在二级缓存中读取到原始值.
     * @param value 二级缓存反序列化后的业务值.
     */
    private record L2Lookup<T>(
        boolean found,
        Optional<T> value) {}

    /**
     * 调用 L3 加载器并写回 L2。
     *
     * <p>加载成功后写 Redis 并记录 stale 兜底值；加载失败时按 loaderFailure 和 stale 策略决定抛出或返回兜底。
     *
     * @param key 业务缓存 key
     * @param staleValue 调用方传入的旧值兜底
     * @param effectiveLoader 当前调用使用的 L3 加载函数
     * @return L3 加载结果或失败策略返回值
     */
    private Optional<V> loadFromL3(K key, Optional<V> staleValue, Supplier<V> effectiveLoader) {
        try {
            countHit("L3");
            // L3 是真实数据源加载，使用 Timer 统计加载耗时用于容量评估。
            V value = loadTimer != null ? loadTimer.record(effectiveLoader::get) : effectiveLoader.get();
            writeL2(key, value);
            rememberStale(key, value);
            return Optional.ofNullable(value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            return handleLoaderFailure(key, staleValue, e);
        }
    }

    /**
     * 按配置处理 L3 加载失败。
     *
     * <p>单 key 和批量回源共用该策略，避免批量 loader 异常绕过 stale/empty 降级语义。
     *
     * @param key 业务缓存 key
     * @param staleValue 调用方传入的旧值兜底
     * @param e 原始加载异常
     * @return 失败策略返回值
     */
    private Optional<V> handleLoaderFailure(K key, Optional<V> staleValue, Exception e) {
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

    /**
     * 使用 Redis 分布式锁保护 L3 加载。
     *
     * <p>拿到锁的节点负责回源并回填 L2，未拿到锁的节点短暂等待 L2；等待失败后再按普通 L3 失败策略处理。
     *
     * @param key 业务缓存 key
     * @param staleValue 调用方传入的旧值兜底
     * @param effectiveLoader 当前调用使用的 L3 加载函数
     * @return L3 加载结果、等待到的 L2 值或失败策略返回值
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
     * 只释放当前线程持有的 Redis 分布式锁。
     *
     * <p>通过 Lua 比较 token 后删除，避免误删其他节点在锁过期后重新获得的新锁。
     *
     * @param lockKey Redis 锁 key
     * @param token 当前加载请求生成的锁 token
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            log.warn("[TIERED_CACHE][{}] Redis lock release failed: {}", name, e.getMessage());
        }
    }

    /**
     * 等待持锁节点回填 L2 后重试读取。
     *
     * <p>最多短暂轮询 Redis；等待期间读到值则直接返回，超时或异常时由当前请求回源 L3。
     *
     * @param key 业务缓存 key
     * @param staleValue 调用方传入的旧值兜底
     * @param effectiveLoader 当前调用使用的 L3 加载函数
     * @return 等待到的 L2 值或后续 L3 加载结果
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
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (Exception ignored) {
                break;
            }
        }
        return loadFromL3(key, staleValue, effectiveLoader);
    }

    /**
     * 写入 L2 Redis 缓存。
     *
     * <p>null 值按穿透策略写入空值哨兵，空集合按空结果 TTL 写入，正常值使用带抖动的 L2 TTL。
     *
     * @param key 业务缓存 key
     * @param value 待写入的业务值
     * @return true 表示 Redis 写入成功或按策略无需写入
     */
    private boolean writeL2(K key, V value) {
        try {
            if (value == null) {
                if (cacheNullValues()) {
                    redis.opsForValue().set(l2Key(key), NULL_SENTINEL, nullValueTtl);
                }
            // 根据前序判断结果进入后续条件分支。
            } else if (isEmptyValue(value) && cacheEmptyValues()) {
                redis.opsForValue().set(l2Key(key), serialize(value), emptyValueTtl);
            } else {
                redis.opsForValue().set(l2Key(key), serialize(value), actualL2Ttl());
            }
            return true;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            if (redisWriteFailure == RedisFailureStrategy.FAIL_FAST) {
                throw asRuntime(e);
            }
            log.warn("[TIERED_CACHE][{}] Redis write failed: {}", name, e.getMessage());
            return false;
        }
    }

    /**
     * 删除 L2 Redis 缓存。
     *
     * <p>Redis 删除失败时按 redisWriteFailure 决定快速失败或记录告警后继续。
     *
     * @param key 需要删除的业务缓存 key
     */
    private void deleteL2(K key) {
        try {
            redis.delete(l2Key(key));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            if (redisWriteFailure == RedisFailureStrategy.FAIL_FAST) {
                throw asRuntime(e);
            }
            log.warn("[TIERED_CACHE][{}] Redis delete failed: {}", name, e.getMessage());
        }
    }

    /**
     * 提升指定 key 的失效版本号。
     *
     * <p>版本号存放在 Redis 中，用于判断本节点 L1 是否落后；Redis 写失败时按策略快速失败或使用时间戳兜底。
     *
     * @param key 业务缓存 key
     * @return 新的失效版本号
     */
    private long bumpInvalidationVersion(K key) {
        try {
            String versionKey = invalidationVersionKey(key);
            Long version = redis.opsForValue().increment(versionKey);
            redis.expire(versionKey, invalidationVersionTtl());
            return version != null ? version : System.currentTimeMillis();
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            if (redisWriteFailure == RedisFailureStrategy.FAIL_FAST) {
                throw asRuntime(e);
            }
            log.warn("[TIERED_CACHE][{}] invalidation version bump failed key={}: {}", name, key, e.getMessage());
            return System.currentTimeMillis();
        }
    }

    /**
     * 计算失效版本键的 TTL，确保版本至少覆盖所有可能仍可读取旧值的缓存窗口。
     *
     * @return 失效版本 Redis 键过期时间
     */
    private Duration invalidationVersionTtl() {
        Duration max = maxDuration(l2Ttl, staleTtl);
        max = maxDuration(max, nullValueTtl);
        max = maxDuration(max, emptyValueTtl);
        max = maxDuration(max, refreshAhead);
        return max.plus(Duration.ofHours(1));
    }

    /**
     * 在两个可空时间段中取较大值。
     *
     * @param first 第一个时间段
     * @param second 第二个时间段
     * @return 两者中的较大值，均为空时返回零时长
     */
    private static Duration maxDuration(Duration first, Duration second) {
        if (first == null) return second == null ? Duration.ZERO : second;
        if (second == null) return first;
        return first.compareTo(second) >= 0 ? first : second;
    }

    /**
     * 读取指定 key 当前的失效版本号。
     *
     * <p>版本缺失视为 0；版本值损坏或 Redis 读取降级时返回 null，调用方会避免信任本地 L1 版本。
     *
     * @param key 业务缓存 key
     * @return 当前失效版本号，无法可靠读取时为 null
     */
    private Long currentInvalidationVersion(K key) {
        try {
            String version = redis.opsForValue().get(invalidationVersionKey(key));
            return version == null || version.isBlank() ? 0L : Long.parseLong(version);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException e) {
            log.warn("[TIERED_CACHE][{}] invalidation version is invalid key={}: {}", name, key, e.getMessage());
            return null;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            if (redisReadFailure == RedisFailureStrategy.FAIL_FAST) {
                throw asRuntime(e);
            }
            log.warn("[TIERED_CACHE][{}] invalidation version read failed key={}: {}", name, key, e.getMessage());
            return null;
        }
    }

    /**
     * 读取仍然新鲜的 L1 本地缓存值。
     *
     * <p>只有本地记录版本不低于 Redis 当前失效版本时才返回缓存；版本落后会立即清理本地值。
     *
     * @param key 业务缓存 key
     * @return 新鲜的 L1 值；本地未命中或版本过期时返回 null
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
     * 使用 Redis 当前失效版本写入 L1。
     *
     * <p>当前版本不可读时仍写入 L1，但移除本地版本记录，后续读取不会把该值当作可靠新鲜值。
     *
     * @param key 业务缓存 key
     * @param value 待写入 L1 的 Optional 值
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
     * 写入 L1 并记录对应失效版本。
     *
     * <p>版本记录用于后续 L1 命中时判断是否落后于跨节点失效事件。
     *
     * @param key 业务缓存 key
     * @param value 待写入 L1 的 Optional 值
     * @param version 写入时观察到的失效版本
     */
    private void putL1(K key, Optional<V> value, long version) {
        l1.put(key, value);
        l1Versions.put(key, version);
        cleanupDetachedLocalVersions();
    }

    /**
     * 计算 L1 的纳秒级 TTL。
     *
     * <p>L1 正常值沿用 L2 TTL，null 和空结果使用各自短 TTL，避免穿透占位长期停留在本地。
     *
     * @param value 待写入 L1 的 Optional 值
     * @return Caffeine Expiry 需要的纳秒 TTL
     */
    private long l1TtlNanos(Optional<V> value) {
        Duration ttl;
        if (value == null || value.isEmpty()) {
            ttl = nullValueTtl;
        // 根据前序判断结果进入后续条件分支。
        } else if (isEmptyValue(value.get())) {
            ttl = emptyValueTtl;
        } else {
            ttl = l2Ttl;
        }
        return Math.max(1L, ttl.toNanos());
    }

    /**
     * 记录当前 key 的本地 L1 版本。
     *
     * <p>Redis 版本可读时保存版本；不可读时移除本地版本，后续 L1 命中会被视为不可靠。
     *
     * @param key 业务缓存 key
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
     * 清理本节点 L1 缓存和本地版本记录。
     *
     * <p>该方法不删除 Redis，也不发布跨节点事件，适合内部局部清理或广播消费。
     *
     * @param key 业务缓存 key
     */
    private void invalidateLocal(K key) {
        l1.invalidate(key);
        l1Versions.remove(key);
    }

    /**
     * 清理已经没有 L1 条目的本地版本记录。
     *
     * <p>版本表超过 L1 最大容量两倍时触发压缩，避免 Caffeine 淘汰后版本记录长期增长。
     */
    private void cleanupDetachedLocalVersions() {
        if (l1Versions.size() <= l1MaxSize * 2L) {
            return;
        }
        l1Versions.keySet().removeIf(key -> !l1.asMap().containsKey(key));
    }

    /**
     * 发布版本化缓存失效事件。
     *
     * <p>事件携带 cacheName、rawKey 和版本号，由 manager 或外部通道传播给其他节点清理 L1。
     *
     * @param rawKey 原始业务 key 字符串
     * @param version 失效版本号
     */
    private void publishInvalidate(String rawKey, long version) {
        if (invalidationPublisher != null) {
            invalidationPublisher.publish(new CacheInvalidationEvent(name, rawKey, version));
        }
    }

    /**
     * 返回当前缓存的 Redis 失效频道名。
     *
     * <p>频道名由缓存名称派生，用于同 Redis 集群内广播 L1 失效消息。
     *
     * @return Redis Pub/Sub 失效频道名
     */
    String invalidateChannel() {
        return "tiered-cache:" + name + ":invalidate";
    }

    /**
     * 生成 L2 Redis 数据 key。
     *
     * <p>key 包含前缀、schema 版本和业务 key 字符串，便于版本升级时隔离旧缓存。
     *
     * @param key 业务缓存 key
     * @return Redis 数据 key
     */
    String l2Key(K key) {
        return l2KeyPrefix + "v" + keySchemaVersion + ":" + keyToString(key);
    }

    /**
     * 生成 Redis 失效版本 key。
     *
     * <p>版本 key 与数据 key 使用同一 schema 版本，确保缓存规则升级后版本空间也同步隔离。
     *
     * @param key 业务缓存 key
     * @return Redis 失效版本 key
     */
    String invalidationVersionKey(K key) {
        return l2KeyPrefix + "v" + keySchemaVersion + ":__invalidate__:" + keyToString(key);
    }

    /**
     * 将业务 key 转换为 Redis key 片段。
     *
     * <p>当前实现使用 {@link String#valueOf(Object)}，调用方应保证业务 key 的字符串表达稳定。
     *
     * @param key 业务缓存 key
     * @return 业务 key 字符串
     */
    private String keyToString(K key) {
        return String.valueOf(key);
    }

    /**
     * 计算本次写入 L2 的实际 TTL。
     *
     * <p>开启 TTL 抖动时会在基础 TTL 上增加随机延迟，用于分散缓存过期时间。
     *
     * @return 实际 L2 TTL
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
     * 将业务值序列化为 L2 JSON。
     *
     * <p>序列化失败表示缓存值类型或 ObjectMapper 配置异常，会直接抛出以暴露配置问题。
     *
     * @param value 待写入 Redis 的业务值
     * @return JSON 字符串
     */
    private String serialize(V value) {
        try {
            return objectMapper.writeValueAsString(value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalStateException("Cache serialize failed", e);
        }
    }

    /**
     * 将 L2 JSON 反序列化为业务值。
     *
     * <p>该方法用于明确要求成功解析的路径，失败时直接抛出运行时异常。
     *
     * @param json Redis 中保存的 JSON
     * @return 业务值
     */
    private V deserialize(String json) {
        try {
            return objectMapper.readValue(json, valueJavaType);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalStateException("Cache deserialize failed", e);
        }
    }

    /**
     * 反序列化 L2 JSON，并按策略处理失败。
     *
     * <p>配置为 THROW 时直接抛出；默认策略会删除损坏的 L2 数据并返回 null，让调用链路继续回源。
     *
     * @param key 业务缓存 key
     * @param json Redis 中保存的 JSON
     * @return 业务值，解析失败且允许降级时为 null
     */
    private V deserializeWithFallback(K key, String json) {
        try {
            return objectMapper.readValue(json, valueJavaType);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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
     * 将异常统一转换为 RuntimeException。
     *
     * <p>已有运行时异常会原样返回，受检异常包装后用于缓存失败策略的快速失败路径。
     *
     * @param e 原始异常
     * @return 运行时异常
     */
    private RuntimeException asRuntime(Exception e) {
        return e instanceof RuntimeException runtime ? runtime : new RuntimeException(e);
    }

    /**
     * 注册 Micrometer 缓存指标。
     *
     * <p>指标按 cache name 和层级打标签，覆盖 L1/L2/L3 命中、未命中、加载耗时、穿透和热点等待等维度。
     *
     * @param registry 指标注册表
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
     * 记录指定缓存层级的命中。
     *
     * <p>同时更新 Micrometer counter 和本地 LongAdder，后者用于 stats 快照。
     *
     * @param level 缓存层级，支持 L1、L2、L3
     */
    private void countHit(String level) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if ("L1".equals(level)) { increment(l1Hits); localL1Hits.increment(); }
        if ("L2".equals(level)) { increment(l2Hits); localL2Hits.increment(); }
        if ("L3".equals(level)) { increment(l3Hits); localL3Loads.increment(); }
    }

    /**
     * 记录指定缓存层级的未命中。
     *
     * <p>当前只统计 L1 和 L2 未命中，L3 通过加载命中计数表达回源次数。
     *
     * @param level 缓存层级，支持 L1、L2
     */
    private void countMiss(String level) {
        if ("L1".equals(level)) { increment(l1Misses); localL1Misses.increment(); }
        if ("L2".equals(level)) { increment(l2Misses); localL2Misses.increment(); }
    }

    /**
     * 安全递增可选 Micrometer counter。
     *
     * <p>指标未启用时 counter 为 null，此方法会直接跳过。
     *
     * @param counter 待递增的指标计数器
     */
    private void increment(Counter counter) {
        if (counter != null) {
            counter.increment();
        }
    }

    /**
     * 判断 key 是否应被穿透保护拦截。
     *
     * <p>根据策略组合执行 keyValidator 和 bloomFilter，命中拦截时增加穿透拒绝计数并跳过 L2/L3。
     *
     * @param key 业务缓存 key
     * @return true 表示本次请求应直接返回空结果
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
     * 判断是否缓存 null 占位。
     *
     * <p>由穿透保护策略决定，开启后 null 会写入 L2 哨兵并使用 nullValueTtl。
     *
     * @return true 表示缓存 null 占位
     */
    private boolean cacheNullValues() {
        return penetration == PenetrationProtectionStrategy.CACHE_NULL_SHORT_TTL
                || penetration == PenetrationProtectionStrategy.CACHE_NULL_AND_BLOOM
                || penetration == PenetrationProtectionStrategy.FULL;
    }

    /**
     * 判断是否缓存空集合或空 Optional。
     *
     * <p>由穿透保护策略决定，开启后空结果会使用 emptyValueTtl 写入 L2。
     *
     * @return true 表示缓存空结果
     */
    private boolean cacheEmptyValues() {
        return penetration == PenetrationProtectionStrategy.CACHE_EMPTY_SHORT_TTL
                || penetration == PenetrationProtectionStrategy.FULL;
    }

    /**
     * 判断是否启用本地 single-flight。
     *
     * <p>开启后同一 JVM 内相同 key 的并发回源会合并为一次加载。
     *
     * @return true 表示启用本地并发合并
     */
    private boolean useLocalSingleFlight() {
        return breakdown == BreakdownProtectionStrategy.LOCAL_SINGLE_FLIGHT
                || breakdown == BreakdownProtectionStrategy.LOCAL_AND_DISTRIBUTED
                || breakdown == BreakdownProtectionStrategy.FULL;
    }

    /**
     * 判断是否启用 Redis 分布式加载锁。
     *
     * <p>热点保护或分布式击穿策略开启时使用，用于收敛多节点同时回源。
     *
     * @return true 表示启用分布式加载锁
     */
    private boolean useDistributedLock() {
        return hotspotProtection
                || breakdown == BreakdownProtectionStrategy.DISTRIBUTED_LOCK
                || breakdown == BreakdownProtectionStrategy.LOCAL_AND_DISTRIBUTED
                || breakdown == BreakdownProtectionStrategy.FULL;
    }

    /**
     * 判断加载失败时是否允许返回旧值。
     *
     * <p>雪崩 stale 策略或 stale-while-revalidate 击穿策略开启时，本地 staleValues 可作为可用性兜底。
     *
     * @return true 表示允许旧值兜底
     */
    private boolean useStaleOnError() {
        return avalanche == AvalancheProtectionStrategy.STALE_ON_ERROR
                || avalanche == AvalancheProtectionStrategy.FULL
                || breakdown == BreakdownProtectionStrategy.STALE_WHILE_REVALIDATE
                || breakdown == BreakdownProtectionStrategy.FULL;
    }

    /**
     * 判断是否启用 L2 TTL 抖动。
     *
     * <p>TTL 抖动用于分散 Redis key 过期时间，降低缓存雪崩风险。
     *
     * @return true 表示写 L2 时应用 TTL 抖动
     */
    private boolean useTtlJitter() {
        return avalanche == AvalancheProtectionStrategy.TTL_JITTER
                || avalanche == AvalancheProtectionStrategy.FULL;
    }

    /**
     * 判断业务值是否为空结果。
     *
     * <p>支持空 Collection、空 Map 和 Optional.empty，用于决定是否使用 emptyValueTtl。
     *
     * @param value 业务值
     * @return true 表示业务值语义为空
     */
    private boolean isEmptyValue(V value) {
        return value instanceof Collection<?> collection && collection.isEmpty()
                || value instanceof Map<?, ?> map && map.isEmpty()
                || value instanceof Optional<?> optional && optional.isEmpty();
    }

    /**
     * 保存最近一次成功加载的旧值。
     *
     * <p>旧值用于加载器异常时兜底；同时把 key 登记到布隆过滤器，降低后续穿透误拦截。
     *
     * @param key 业务缓存 key
     * @param value 最近一次成功加载的非 null 值
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
     * 获取仍在可用窗口内的旧值。
     *
     * <p>超过 staleTtl 的旧值会被移除并视为不可用，避免长期返回过期数据。
     *
     * @param key 业务缓存 key
     * @return 可用旧值
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
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (RuntimeException e) {
                marker.completeExceptionally(e);
                log.warn("[TIERED_CACHE][{}] refreshAhead failed key={}: {}", name, key, e.getMessage());
            } finally {
                inFlightLoads.remove(key, marker);
            }
        });
    }

    /**
     * StaleValue record.
     * @param value 可用于异常兜底或提前刷新的旧值.
     * @param createdAt 旧值写入本地兜底区的时间.
     */
    private record StaleValue<T>(
        T value,
        Instant createdAt) {}

    /**
     * ReactiveTieredCacheView 封装本模块的核心职责、输入输出结构和协作边界。
     */
    private static class ReactiveTieredCacheView<K, V> implements ReactiveTieredCache<K, V> {
        /** 被包装为响应式接口的同步缓存实现。 */
        private final TieredCacheImpl<K, V> delegate;

        /**
         * 创建同步缓存的响应式包装视图。
         *
         * <p>包装视图共享 delegate 的 L1 状态、Redis key 规则和失败策略，不维护独立缓存副本。
         *
         * @param delegate 被包装的分层缓存实现
         */
        ReactiveTieredCacheView(TieredCacheImpl<K, V> delegate) {
            this.delegate = delegate;
        }

        /**
         * 响应式读取单个 key。
         *
         * <p>先同步检查 L1；存在 reactive Redis 时异步读取 L2，未配置时退到 boundedElastic 执行同步 get。
         *
         * @param key 业务缓存 key
         * @return 订阅后产生的缓存读取结果
         */
        @Override
        public Mono<Optional<V>> get(K key) {
            Optional<V> cached = delegate.freshL1IfPresent(key);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
                    // 遍历候选数据并按业务规则筛选、转换或聚合。
                    .flatMap(json -> {
                        L2Lookup<V> l2Value = delegate.handleL2Json(key, json, true);
                        // 汇总前面计算出的状态和明细，返回给调用方。
                        return Mono.just(l2Value.value());
                    })
                    .switchIfEmpty(Mono.fromCallable(() -> delegate.hotspotProtection
                                    ? delegate.loadFromL3WithLock(key, null, () -> delegate.loader.apply(key))
                                    : delegate.loadFromL3(key, null, () -> delegate.loader.apply(key)))
                            .subscribeOn(Schedulers.boundedElastic()));
        }

        /**
         * 响应式读取已存在的缓存值。
         *
         * <p>该路径不触发 L3 加载器；未配置 reactive Redis 时退到 boundedElastic 执行同步 getIfPresent。
         *
         * @param key 业务缓存 key
         * @return 订阅后产生的已缓存值
         */
        @Override
        public Mono<Optional<V>> getIfPresent(K key) {
            Optional<V> cached = delegate.freshL1IfPresent(key);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            return redisGet
                    // 遍历候选数据并按业务规则筛选、转换或聚合。
                    .map(json -> delegate.handleL2Json(key, json, true).value())
                    .defaultIfEmpty(Optional.empty());
        }

        /**
         * 响应式写入缓存值。
         *
         * <p>写入复用同步 put 的 L1/L2 更新和失效广播语义，并放到 boundedElastic 避免阻塞响应式调用线程。
         *
         * @param key 业务缓存 key
         * @param value 待缓存的业务值
         * @return 写入完成信号
         */
        @Override
        public Mono<Void> put(K key, V value) {
            return Mono.fromRunnable(() -> delegate.put(key, value)).subscribeOn(Schedulers.boundedElastic()).then();
        }

        /**
         * 响应式失效缓存 key。
         *
         * <p>失效复用同步 invalidate 的本地清理、Redis 删除和跨节点广播，并放到 boundedElastic 执行。
         *
         * @param key 需要失效的业务缓存 key
         * @return 失效完成信号
         */
        @Override
        public Mono<Void> invalidate(K key) {
            return Mono.fromRunnable(() -> delegate.invalidate(key)).subscribeOn(Schedulers.boundedElastic()).then();
        }

        /**
         * 响应式刷新缓存 key。
         *
         * <p>刷新复用同步 refresh 的删除旧值、回源加载和广播逻辑，并放到 boundedElastic 执行。
         *
         * @param key 需要刷新的业务缓存 key
         * @return 刷新完成信号
         */
        @Override
        public Mono<Void> refresh(K key) {
            return Mono.fromRunnable(() -> delegate.refresh(key)).subscribeOn(Schedulers.boundedElastic()).then();
        }
    }
}
