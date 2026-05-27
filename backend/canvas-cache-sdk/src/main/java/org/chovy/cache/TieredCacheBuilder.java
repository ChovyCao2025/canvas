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
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
    private TieredCacheBuilder() {}

    /**
     * 配置 builder 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public static <K, V> TieredCacheBuilder<K, V> builder() {
        return new TieredCacheBuilder<>();
    }

    /**
     * 配置 name 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> name(String value) { this.name = value; return this; }
    /**
     * 配置 l1 Max Size 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> l1MaxSize(int value) { this.l1MaxSize = value; return this; }
    /**
     * 配置 l1 Refresh After Write 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> l1RefreshAfterWrite(Duration value) { this.l1RefreshAfterWrite = value; return this; }
    /**
     * 配置 l2 Key Prefix 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> l2KeyPrefix(String value) { this.l2KeyPrefix = value; return this; }
    /**
     * 配置 l2 Ttl 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> l2Ttl(Duration value) { this.l2Ttl = value; return this; }
    /**
     * 配置 l2 Ttl Jitter 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> l2TtlJitter(double value) { this.l2TtlJitter = value; return this; }
    /**
     * 配置 key Schema Version 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> keySchemaVersion(int value) { this.keySchemaVersion = value; return this; }
    /**
     * 配置 null Value Ttl 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> nullValueTtl(Duration value) { this.nullValueTtl = value; return this; }
    /**
     * 配置 empty Value Ttl 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> emptyValueTtl(Duration value) { this.emptyValueTtl = value; return this; }
    /**
     * 配置 lock Ttl 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> lockTtl(Duration value) { this.lockTtl = value; return this; }
    /**
     * 配置 refresh Ahead 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> refreshAhead(Duration value) { this.refreshAhead = value; return this; }
    /**
     * 配置 stale Ttl 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> staleTtl(Duration value) { this.staleTtl = value; return this; }
    /**
     * 配置 hotspot Protection 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
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
     * 配置 penetration 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> penetration(PenetrationProtectionStrategy value) { this.penetration = value; return this; }
    /**
     * 配置 breakdown 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> breakdown(BreakdownProtectionStrategy value) { this.breakdown = value; return this; }
    /**
     * 配置 avalanche 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> avalanche(AvalancheProtectionStrategy value) { this.avalanche = value; return this; }
    /**
     * 配置 key Validator 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> keyValidator(Predicate<K> value) { this.keyValidator = value; return this; }
    /**
     * 配置 bloom Filter 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> bloomFilter(CacheBloomFilter<K> value) { this.bloomFilter = value; return this; }
    /**
     * 配置 on Loader Failure 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> onLoaderFailure(LoaderFailureStrategy value) { this.loaderFailure = value; return this; }
    /**
     * 配置 on Redis Read Failure 参数，并返回当前构建器以继续链式设置。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> onRedisReadFailure(RedisFailureStrategy value) { this.redisReadFailure = value; return this; }
    /**
     * 配置 on Redis Write Failure 参数，并返回当前构建器以继续链式设置。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> onRedisWriteFailure(RedisFailureStrategy value) { this.redisWriteFailure = value; return this; }
    /**
     * 配置 on Deserialize Failure 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> onDeserializeFailure(DeserializeFailureStrategy value) { this.deserializeFailure = value; return this; }
    /**
     * 配置 loader 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> loader(Function<K, V> value) { this.loader = value; return this; }

    /**
     * 配置 value Type 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> valueType(Class<?> value) {
        this.configuredValueType = value;
        this.valueJavaType = objectMapper.constructType(value);
        return this;
    }

    /**
     * 配置 value Type 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> valueType(TypeReference<?> value) {
        this.configuredValueType = value.getType();
        this.valueJavaType = objectMapper.constructType(configuredValueType);
        return this;
    }

    /**
     * 配置 object Mapper 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
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
     * 配置 enable Metrics 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> enableMetrics(boolean value) { this.enableMetrics = value; return this; }
    /**
     * 配置 meter Registry 参数，并返回当前构建器以继续链式设置。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public TieredCacheBuilder<K, V> meterRegistry(MeterRegistry value) { this.meterRegistry = value; return this; }

    /**
     * 构建、解析或转换 build 相关的业务数据。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param manager manager 方法执行所需的业务参数
     * @return 方法执行后的业务结果
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
