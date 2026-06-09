package org.chovy.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.chovy.cache.strategy.DeserializeFailureStrategy;
import org.chovy.cache.strategy.LoaderFailureStrategy;
import org.chovy.cache.strategy.RedisFailureStrategy;
import org.chovy.cache.strategy.AvalancheProtectionStrategy;
import org.chovy.cache.strategy.BreakdownProtectionStrategy;
import org.chovy.cache.strategy.PenetrationProtectionStrategy;

import java.time.Duration;
import java.lang.reflect.Type;
import java.util.function.Predicate;
import java.util.function.Function;

/**
 * 分层缓存构建器，集中收集缓存名称、TTL、加载器、序列化和保护策略等配置。
 *
 * <p>使用构建器可以避免调用方直接依赖 TieredCacheImpl 构造细节，并保证默认策略统一。
 * <p>build 后得到可直接注册到 TieredCacheManager 的缓存实例。
 */
public class TieredCacheBuilder<K, V> {
    /** 缓存实例名称。 */
    private String name;
    /** 一级本地缓存最大条目数。 */
    private int l1MaxSize = 1000;
    /** 一级缓存写入后的刷新间隔。 */
    private Duration l1RefreshAfterWrite = Duration.ofHours(1);
    /** 二级 Redis 缓存键前缀。 */
    private String l2KeyPrefix = "cache:";
    /** 二级 Redis 缓存默认过期时间。 */
    private Duration l2Ttl = Duration.ofHours(24);
    /** 二级缓存过期时间抖动比例。 */
    private double l2TtlJitter = 0.1;
    /** 缓存键结构版本号。 */
    private int keySchemaVersion = 1;
    /** 空对象占位值过期时间。 */
    private Duration nullValueTtl = Duration.ofMinutes(1);
    /** 空集合或空结果占位值过期时间。 */
    private Duration emptyValueTtl = Duration.ofMinutes(1);
    /** 分布式加载锁过期时间。 */
    private Duration lockTtl = Duration.ofSeconds(30);
    /** 缓存过期前提前刷新窗口。 */
    private Duration refreshAhead = Duration.ZERO;
    /** 旧值兜底可用时间。 */
    private Duration staleTtl = Duration.ofMinutes(30);
    /** 是否启用热点 key 保护。 */
    private boolean hotspotProtection = false;
    /** 缓存穿透保护策略。 */
    private PenetrationProtectionStrategy penetration = PenetrationProtectionStrategy.CACHE_NULL_SHORT_TTL;
    /** 缓存击穿保护策略。 */
    private BreakdownProtectionStrategy breakdown = BreakdownProtectionStrategy.LOCAL_SINGLE_FLIGHT;
    /** 缓存雪崩保护策略。 */
    private AvalancheProtectionStrategy avalanche = AvalancheProtectionStrategy.TTL_JITTER;
    /** 缓存 key 合法性校验器。 */
    private Predicate<K> keyValidator = key -> true;
    /** 用于拦截不存在 key 的布隆过滤器。 */
    private CacheBloomFilter<K> bloomFilter;
    /** 数据加载器失败时的处理策略。 */
    private LoaderFailureStrategy loaderFailure = LoaderFailureStrategy.THROW;
    /** Redis 读取失败时的处理策略。 */
    private RedisFailureStrategy redisReadFailure = RedisFailureStrategy.FALLTHROUGH;
    /** Redis 写入失败时的处理策略。 */
    private RedisFailureStrategy redisWriteFailure = RedisFailureStrategy.FALLTHROUGH;
    /** Redis 值反序列化失败时的处理策略。 */
    private DeserializeFailureStrategy deserializeFailure = DeserializeFailureStrategy.FALLTHROUGH_TO_L3;
    /** L3 数据加载函数。 */
    private Function<K, V> loader;
    /** 缓存值的 Jackson JavaType。 */
    private JavaType valueJavaType;
    /** 调用方显式配置的缓存值类型。 */
    private Type configuredValueType;
    /** 缓存值序列化与反序列化使用的 ObjectMapper。 */
    private ObjectMapper objectMapper = new ObjectMapper();
    /** 是否注册并上报缓存指标。 */
    private boolean enableMetrics = true;
    /** 缓存指标注册表。 */
    private MeterRegistry meterRegistry;

    /**
     * 构造 TieredCacheBuilder 实例。
     *
     * <p>请通过 {@link #builder()} 创建，确保默认 TTL、穿透/击穿/雪崩保护和失败策略一致。
     */
    private TieredCacheBuilder() {}

    /**
     * 创建一个带默认分层缓存策略的构建器。
     *
     * <p>默认启用短 TTL 空值缓存、L1 单机 single-flight、L2 TTL 抖动和 Redis 失败降级回源。
     *
     * @return 新的缓存构建器
     */
    public static <K, V> TieredCacheBuilder<K, V> builder() {
        return new TieredCacheBuilder<>();
    }

    /**
     * 设置缓存实例名称。
     *
     * <p>名称会用于管理器注册、指标标签、Redis key 前缀的一部分和跨节点失效频道，应保持稳定且唯一。
     *
     * @param value 缓存实例名称
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> name(String value) { this.name = value; return this; }
    /**
     * 设置 L1 本地缓存最大条目数。
     *
     * <p>该限制只作用于当前 JVM 内的 Caffeine 缓存，不影响 Redis 中的 L2 数据容量。
     *
     * @param value L1 最大条目数，必须大于 0
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> l1MaxSize(int value) { this.l1MaxSize = value; return this; }
    /**
     * 设置 L1 写入后的异步刷新间隔。
     *
     * <p>Caffeine 在读取热点 key 时会按该间隔触发 refresh，刷新逻辑仍会优先检查 L2，再按需回源 L3。
     *
     * @param value L1 刷新间隔，必须为正数
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> l1RefreshAfterWrite(Duration value) { this.l1RefreshAfterWrite = value; return this; }
    /**
     * 设置 L2 Redis key 前缀。
     *
     * <p>最终 Redis key 会包含该前缀、缓存名称、key schema 版本和业务 key，用于隔离不同缓存域。
     *
     * @param value Redis key 前缀
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> l2KeyPrefix(String value) { this.l2KeyPrefix = value; return this; }
    /**
     * 设置 L2 Redis 正常值 TTL。
     *
     * <p>业务值写入 Redis 时使用该 TTL；空值和空集合会分别使用 nullValueTtl、emptyValueTtl。
     *
     * @param value L2 正常值过期时间，必须为正数
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> l2Ttl(Duration value) { this.l2Ttl = value; return this; }
    /**
     * 设置 L2 TTL 抖动比例。
     *
     * <p>开启雪崩保护时，实际 Redis TTL 会在基础 TTL 上增加随机抖动，避免大量 key 同时过期。
     *
     * @param value 抖动比例，范围 0 到 1
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> l2TtlJitter(double value) { this.l2TtlJitter = value; return this; }
    /**
     * 设置缓存 key 结构版本。
     *
     * <p>当业务 key 拼接规则或序列化兼容性变化时提升版本，可以让新旧 Redis 数据自然隔离。
     *
     * @param value key schema 版本号
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> keySchemaVersion(int value) { this.keySchemaVersion = value; return this; }
    /**
     * 设置 null 业务值的短 TTL。
     *
     * <p>穿透保护缓存 null 占位时使用该 TTL，既减少无效回源，也避免长期屏蔽后续新增数据。
     *
     * @param value null 占位过期时间，必须为正数
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> nullValueTtl(Duration value) { this.nullValueTtl = value; return this; }
    /**
     * 设置空集合或空结果的短 TTL。
     *
     * <p>用于缓存非 null 但语义为空的结果，降低空查询压力，同时保留较快恢复窗口。
     *
     * @param value 空结果过期时间，必须为正数
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> emptyValueTtl(Duration value) { this.emptyValueTtl = value; return this; }
    /**
     * 设置分布式加载锁 TTL。
     *
     * <p>启用分布式击穿保护时，Redis 锁超过该时间会自动释放，避免加载节点异常导致其他节点永久等待。
     *
     * @param value 分布式锁过期时间，必须为正数
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> lockTtl(Duration value) { this.lockTtl = value; return this; }
    /**
     * 设置提前刷新窗口。
     *
     * <p>该值预留给接近过期时的主动刷新策略，必须非负；为 0 时不启用提前刷新窗口。
     *
     * @param value 提前刷新窗口，必须非负
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> refreshAhead(Duration value) { this.refreshAhead = value; return this; }
    /**
     * 设置旧值兜底可用时间。
     *
     * <p>启用加载失败返回旧值策略时，最近一次成功加载的值会在该窗口内作为失败兜底。
     *
     * @param value 旧值兜底 TTL，必须为正数
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> staleTtl(Duration value) { this.staleTtl = value; return this; }
    /**
     * 开关热点 key 保护。
     *
     * <p>开启后会把默认击穿策略升级为本地加分布式互斥，降低热点 key 在多节点同时回源的概率。
     *
     * @param value 是否启用热点 key 保护
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> hotspotProtection(boolean value) {
        this.hotspotProtection = value;
        if (value && this.breakdown == BreakdownProtectionStrategy.LOCAL_SINGLE_FLIGHT) {
            // 开启热点保护时自动升级为本地+分布式互斥，避免多节点同时击穿到加载器。
            this.breakdown = BreakdownProtectionStrategy.LOCAL_AND_DISTRIBUTED;
        }
        return this;
    }
    /**
     * 设置缓存穿透保护策略。
     *
     * <p>该策略决定非法 key、布隆过滤器判空或加载器返回 null 时，是拒绝、短 TTL 缓存空值还是直接回源透传。
     *
     * @param value 穿透保护策略
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> penetration(PenetrationProtectionStrategy value) { this.penetration = value; return this; }
    /**
     * 设置缓存击穿保护策略。
     *
     * <p>该策略决定未命中回源时是否使用本地 single-flight、Redis 分布式锁或组合策略来收敛并发加载。
     *
     * @param value 击穿保护策略
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> breakdown(BreakdownProtectionStrategy value) { this.breakdown = value; return this; }
    /**
     * 设置缓存雪崩保护策略。
     *
     * <p>当前实现主要通过 TTL 抖动分散 L2 key 过期时间，降低同一时间大量回源的风险。
     *
     * @param value 雪崩保护策略
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> avalanche(AvalancheProtectionStrategy value) { this.avalanche = value; return this; }
    /**
     * 设置业务 key 校验器。
     *
     * <p>校验失败的 key 会进入穿透保护分支，可用于拦截明显非法的用户输入或空 key。
     *
     * @param value 业务 key 校验器
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> keyValidator(Predicate<K> value) { this.keyValidator = value; return this; }
    /**
     * 设置布隆过滤器。
     *
     * <p>当过滤器判断 key 不可能存在时，缓存可直接按穿透保护策略处理，避免访问 L3 数据源。
     *
     * @param value 布隆过滤器实现
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> bloomFilter(CacheBloomFilter<K> value) { this.bloomFilter = value; return this; }
    /**
     * 设置 L3 加载器失败策略。
     *
     * <p>该策略决定回源异常时是向调用方抛出、返回空结果，还是在可用时返回最近一次旧值兜底。
     *
     * @param value 加载器失败策略
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> onLoaderFailure(LoaderFailureStrategy value) { this.loaderFailure = value; return this; }
    /**
     * 设置 Redis 读取失败策略。
     *
     * <p>默认降级为继续访问 L3，避免 Redis 短暂不可用时阻断业务读取；也可配置为直接抛出。
     *
     * @param value Redis 读取失败策略
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> onRedisReadFailure(RedisFailureStrategy value) { this.redisReadFailure = value; return this; }
    /**
     * 设置 Redis 写入失败策略。
     *
     * <p>默认记录失败并继续返回业务结果，避免 L2 写入故障影响 L3 查询成功的主流程。
     *
     * @param value Redis 写入失败策略
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> onRedisWriteFailure(RedisFailureStrategy value) { this.redisWriteFailure = value; return this; }
    /**
     * 设置 Redis 值反序列化失败策略。
     *
     * <p>当 L2 中存在脏数据或类型不兼容数据时，可选择删除后回源、直接抛出，或按配置降级。
     *
     * @param value 反序列化失败策略
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> onDeserializeFailure(DeserializeFailureStrategy value) { this.deserializeFailure = value; return this; }
    /**
     * 设置 L3 数据加载器。
     *
     * <p>缓存未命中且穿透/击穿保护允许回源时会调用该函数；返回 null 会按空值缓存策略处理。
     *
     * @param value L3 数据加载函数
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> loader(Function<K, V> value) { this.loader = value; return this; }

    /**
     * 设置缓存值类型。
     *
     * <p>该类型用于 Jackson 反序列化 Redis 中的 L2 JSON 值，必须与 loader 返回值兼容。
     *
     * @param value 缓存值 Java 类型
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> valueType(Class<?> value) {
        this.configuredValueType = value;
        this.valueJavaType = objectMapper.constructType(value);
        return this;
    }

    /**
     * 设置带泛型信息的缓存值类型。
     *
     * <p>用于 List、Map 等泛型值，避免 L2 JSON 反序列化时丢失元素类型。
     *
     * @param value 缓存值类型引用
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> valueType(TypeReference<?> value) {
        this.configuredValueType = value.getType();
        this.valueJavaType = objectMapper.constructType(configuredValueType);
        return this;
    }

    /**
     * 设置缓存值序列化使用的 ObjectMapper。
     *
     * <p>如果此前已经配置 valueType，本方法会用新的 ObjectMapper 重新构造 JavaType，保证读写使用同一类型体系。
     *
     * @param value Jackson ObjectMapper
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> objectMapper(ObjectMapper value) {
        this.objectMapper = value;
        if (configuredValueType != null) {
            // ObjectMapper 变化后重新构造 JavaType，保证后续 Redis 反序列化使用同一类型体系。
            this.valueJavaType = value.constructType(configuredValueType);
        }
        return this;
    }

    /**
     * 开关 Micrometer 指标注册。
     *
     * <p>关闭后不会向 MeterRegistry 注册 L1/L2/L3 计数和大小指标，但 {@link TieredCache#stats()} 仍可读取内部计数。
     *
     * @param value 是否启用指标注册
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> enableMetrics(boolean value) { this.enableMetrics = value; return this; }
    /**
     * 设置当前缓存专用的 MeterRegistry。
     *
     * <p>未设置时会沿用 {@link TieredCacheManager} 中的全局 registry；设置后仅影响当前 builder 构建出的缓存。
     *
     * @param value 指标注册表
     * @return 当前构建器
     */
    public TieredCacheBuilder<K, V> meterRegistry(MeterRegistry value) { this.meterRegistry = value; return this; }

    /**
     * 校验配置并构建缓存实例。
     *
     * <p>构建时注入同步/响应式 Redis、失效发布器和指标注册表，并立即注册到 manager，供注解切面和跨节点失效使用。
     *
     * @param manager 缓存注册中心和 Redis 依赖来源
     * @return 已注册的分层缓存实例
     * @throws IllegalStateException 必填配置缺失或 TTL、抖动比例等参数非法时抛出
     */
    public TieredCache<K, V> build(TieredCacheManager manager) {
        if (name == null || name.isBlank()) throw new IllegalStateException("name is required");
        if (manager == null) throw new IllegalStateException("manager is required");
        if (l1MaxSize <= 0) throw new IllegalStateException("l1MaxSize must be > 0");
        if (l1RefreshAfterWrite == null || l1RefreshAfterWrite.isNegative() || l1RefreshAfterWrite.isZero()) {
            throw new IllegalStateException("l1RefreshAfterWrite must be > 0");
        }
        if (l2Ttl == null || l2Ttl.isNegative() || l2Ttl.isZero()) {
            throw new IllegalStateException("l2Ttl must be > 0");
        }
        if (nullValueTtl == null || nullValueTtl.isNegative() || nullValueTtl.isZero()) {
            throw new IllegalStateException("nullValueTtl must be > 0");
        }
        if (emptyValueTtl == null || emptyValueTtl.isNegative() || emptyValueTtl.isZero()) {
            throw new IllegalStateException("emptyValueTtl must be > 0");
        }
        if (lockTtl == null || lockTtl.isNegative() || lockTtl.isZero()) {
            throw new IllegalStateException("lockTtl must be > 0");
        }
        if (refreshAhead == null || refreshAhead.isNegative()) {
            throw new IllegalStateException("refreshAhead must be >= 0");
        }
        if (staleTtl == null || staleTtl.isNegative() || staleTtl.isZero()) {
            throw new IllegalStateException("staleTtl must be > 0");
        }
        if (l2TtlJitter < 0 || l2TtlJitter > 1) {
            throw new IllegalStateException("l2TtlJitter must be between 0 and 1");
        }
        if (l2KeyPrefix == null) throw new IllegalStateException("l2KeyPrefix is required");
        if (loader == null) throw new IllegalStateException("loader is required");
        if (valueJavaType == null) throw new IllegalStateException("valueType is required");
        // 指标开关优先使用显式传入的 registry，未传入时沿用 manager 的全局 registry。
        MeterRegistry registry = enableMetrics
                ? (meterRegistry != null ? meterRegistry : manager.getMeterRegistry())
                : null;
        // 统一在构建阶段注入 Redis、响应式 Redis、失效发布器和序列化类型，调用方无需直接依赖实现类构造细节。
        TieredCacheImpl<K, V> cache = new TieredCacheImpl<>(
                name, l1MaxSize, l1RefreshAfterWrite, l2KeyPrefix, l2Ttl, l2TtlJitter,
                keySchemaVersion, nullValueTtl, emptyValueTtl, lockTtl, refreshAhead, staleTtl,
                hotspotProtection, penetration, breakdown, avalanche, keyValidator, bloomFilter, loaderFailure,
                redisReadFailure, redisWriteFailure, deserializeFailure, loader,
                valueJavaType, objectMapper, manager.getRedis(), manager.getReactiveRedis(), manager::publish, registry);
        // 构建成功后立即注册，保证注解切面和跨节点失效事件能按 cacheName 找到同一个实例。
        manager.register(cache);
        return cache;
    }
}
