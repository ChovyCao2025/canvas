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

public class TieredCacheBuilder<K, V> {
    private String name;
    private int l1MaxSize = 1000;
    private Duration l1RefreshAfterWrite = Duration.ofHours(1);
    private String l2KeyPrefix = "cache:";
    private Duration l2Ttl = Duration.ofHours(24);
    private double l2TtlJitter = 0.1;
    private int keySchemaVersion = 1;
    private Duration nullValueTtl = Duration.ofMinutes(1);
    private Duration emptyValueTtl = Duration.ofMinutes(1);
    private Duration lockTtl = Duration.ofSeconds(30);
    private Duration refreshAhead = Duration.ZERO;
    private Duration staleTtl = Duration.ofMinutes(30);
    private boolean hotspotProtection = false;
    private PenetrationProtectionStrategy penetration = PenetrationProtectionStrategy.CACHE_NULL_SHORT_TTL;
    private BreakdownProtectionStrategy breakdown = BreakdownProtectionStrategy.LOCAL_SINGLE_FLIGHT;
    private AvalancheProtectionStrategy avalanche = AvalancheProtectionStrategy.TTL_JITTER;
    private Predicate<K> keyValidator = key -> true;
    private CacheBloomFilter<K> bloomFilter;
    private LoaderFailureStrategy loaderFailure = LoaderFailureStrategy.THROW;
    private RedisFailureStrategy redisReadFailure = RedisFailureStrategy.FALLTHROUGH;
    private RedisFailureStrategy redisWriteFailure = RedisFailureStrategy.FALLTHROUGH;
    private DeserializeFailureStrategy deserializeFailure = DeserializeFailureStrategy.FALLTHROUGH_TO_L3;
    private Function<K, V> loader;
    private JavaType valueJavaType;
    private Type configuredValueType;
    private ObjectMapper objectMapper = new ObjectMapper();
    private boolean enableMetrics = true;
    private MeterRegistry meterRegistry;

    private TieredCacheBuilder() {}

    public static <K, V> TieredCacheBuilder<K, V> builder() {
        return new TieredCacheBuilder<>();
    }

    public TieredCacheBuilder<K, V> name(String value) { this.name = value; return this; }
    public TieredCacheBuilder<K, V> l1MaxSize(int value) { this.l1MaxSize = value; return this; }
    public TieredCacheBuilder<K, V> l1RefreshAfterWrite(Duration value) { this.l1RefreshAfterWrite = value; return this; }
    public TieredCacheBuilder<K, V> l2KeyPrefix(String value) { this.l2KeyPrefix = value; return this; }
    public TieredCacheBuilder<K, V> l2Ttl(Duration value) { this.l2Ttl = value; return this; }
    public TieredCacheBuilder<K, V> l2TtlJitter(double value) { this.l2TtlJitter = value; return this; }
    public TieredCacheBuilder<K, V> keySchemaVersion(int value) { this.keySchemaVersion = value; return this; }
    public TieredCacheBuilder<K, V> nullValueTtl(Duration value) { this.nullValueTtl = value; return this; }
    public TieredCacheBuilder<K, V> emptyValueTtl(Duration value) { this.emptyValueTtl = value; return this; }
    public TieredCacheBuilder<K, V> lockTtl(Duration value) { this.lockTtl = value; return this; }
    public TieredCacheBuilder<K, V> refreshAhead(Duration value) { this.refreshAhead = value; return this; }
    public TieredCacheBuilder<K, V> staleTtl(Duration value) { this.staleTtl = value; return this; }
    public TieredCacheBuilder<K, V> hotspotProtection(boolean value) {
        this.hotspotProtection = value;
        if (value && this.breakdown == BreakdownProtectionStrategy.LOCAL_SINGLE_FLIGHT) {
            this.breakdown = BreakdownProtectionStrategy.LOCAL_AND_DISTRIBUTED;
        }
        return this;
    }
    public TieredCacheBuilder<K, V> penetration(PenetrationProtectionStrategy value) { this.penetration = value; return this; }
    public TieredCacheBuilder<K, V> breakdown(BreakdownProtectionStrategy value) { this.breakdown = value; return this; }
    public TieredCacheBuilder<K, V> avalanche(AvalancheProtectionStrategy value) { this.avalanche = value; return this; }
    public TieredCacheBuilder<K, V> keyValidator(Predicate<K> value) { this.keyValidator = value; return this; }
    public TieredCacheBuilder<K, V> bloomFilter(CacheBloomFilter<K> value) { this.bloomFilter = value; return this; }
    public TieredCacheBuilder<K, V> onLoaderFailure(LoaderFailureStrategy value) { this.loaderFailure = value; return this; }
    public TieredCacheBuilder<K, V> onRedisReadFailure(RedisFailureStrategy value) { this.redisReadFailure = value; return this; }
    public TieredCacheBuilder<K, V> onRedisWriteFailure(RedisFailureStrategy value) { this.redisWriteFailure = value; return this; }
    public TieredCacheBuilder<K, V> onDeserializeFailure(DeserializeFailureStrategy value) { this.deserializeFailure = value; return this; }
    public TieredCacheBuilder<K, V> loader(Function<K, V> value) { this.loader = value; return this; }

    public TieredCacheBuilder<K, V> valueType(Class<?> value) {
        this.configuredValueType = value;
        this.valueJavaType = objectMapper.constructType(value);
        return this;
    }

    public TieredCacheBuilder<K, V> valueType(TypeReference<?> value) {
        this.configuredValueType = value.getType();
        this.valueJavaType = objectMapper.constructType(configuredValueType);
        return this;
    }

    public TieredCacheBuilder<K, V> objectMapper(ObjectMapper value) {
        this.objectMapper = value;
        if (configuredValueType != null) {
            this.valueJavaType = value.constructType(configuredValueType);
        }
        return this;
    }

    public TieredCacheBuilder<K, V> enableMetrics(boolean value) { this.enableMetrics = value; return this; }
    public TieredCacheBuilder<K, V> meterRegistry(MeterRegistry value) { this.meterRegistry = value; return this; }

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
        MeterRegistry registry = enableMetrics
                ? (meterRegistry != null ? meterRegistry : manager.getMeterRegistry())
                : null;
        TieredCacheImpl<K, V> cache = new TieredCacheImpl<>(
                name, l1MaxSize, l1RefreshAfterWrite, l2KeyPrefix, l2Ttl, l2TtlJitter,
                keySchemaVersion, nullValueTtl, emptyValueTtl, lockTtl, refreshAhead, staleTtl,
                hotspotProtection, penetration, breakdown, avalanche, keyValidator, bloomFilter, loaderFailure,
                redisReadFailure, redisWriteFailure, deserializeFailure, loader,
                valueJavaType, objectMapper, manager.getRedis(), manager.getReactiveRedis(), registry);
        manager.register(cache);
        return cache;
    }
}
