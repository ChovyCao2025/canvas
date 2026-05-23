package org.chovy.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.chovy.cache.strategy.DeserializeFailureStrategy;
import org.chovy.cache.strategy.LoaderFailureStrategy;
import org.chovy.cache.strategy.RedisFailureStrategy;

import java.time.Duration;
import java.util.function.Function;

public class TieredCacheBuilder<K, V> {
    private String name;
    private int l1MaxSize = 1000;
    private Duration l1RefreshAfterWrite = Duration.ofHours(1);
    private String l2KeyPrefix = "cache:";
    private Duration l2Ttl = Duration.ofHours(24);
    private double l2TtlJitter = 0.1;
    private int keySchemaVersion = 1;
    private Duration nullValueTtl = Duration.ofMinutes(5);
    private boolean hotspotProtection = false;
    private LoaderFailureStrategy loaderFailure = LoaderFailureStrategy.THROW;
    private RedisFailureStrategy redisReadFailure = RedisFailureStrategy.FALLTHROUGH;
    private RedisFailureStrategy redisWriteFailure = RedisFailureStrategy.FALLTHROUGH;
    private DeserializeFailureStrategy deserializeFailure = DeserializeFailureStrategy.FALLTHROUGH_TO_L3;
    private Function<K, V> loader;
    private JavaType valueJavaType;
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
    public TieredCacheBuilder<K, V> hotspotProtection(boolean value) { this.hotspotProtection = value; return this; }
    public TieredCacheBuilder<K, V> onLoaderFailure(LoaderFailureStrategy value) { this.loaderFailure = value; return this; }
    public TieredCacheBuilder<K, V> onRedisReadFailure(RedisFailureStrategy value) { this.redisReadFailure = value; return this; }
    public TieredCacheBuilder<K, V> onRedisWriteFailure(RedisFailureStrategy value) { this.redisWriteFailure = value; return this; }
    public TieredCacheBuilder<K, V> onDeserializeFailure(DeserializeFailureStrategy value) { this.deserializeFailure = value; return this; }
    public TieredCacheBuilder<K, V> loader(Function<K, V> value) { this.loader = value; return this; }

    public TieredCacheBuilder<K, V> valueType(Class<?> value) {
        this.valueJavaType = objectMapper.constructType(value);
        return this;
    }

    public TieredCacheBuilder<K, V> valueType(TypeReference<?> value) {
        this.valueJavaType = objectMapper.constructType(value.getType());
        return this;
    }

    public TieredCacheBuilder<K, V> objectMapper(ObjectMapper value) {
        this.objectMapper = value;
        if (valueJavaType != null) {
            this.valueJavaType = value.constructType(valueJavaType.getRawClass());
        }
        return this;
    }

    public TieredCacheBuilder<K, V> enableMetrics(boolean value) { this.enableMetrics = value; return this; }
    public TieredCacheBuilder<K, V> meterRegistry(MeterRegistry value) { this.meterRegistry = value; return this; }

    public TieredCache<K, V> build(TieredCacheManager manager) {
        if (name == null || name.isBlank()) throw new IllegalStateException("name is required");
        if (loader == null) throw new IllegalStateException("loader is required");
        if (valueJavaType == null) throw new IllegalStateException("valueType is required");
        MeterRegistry registry = enableMetrics
                ? (meterRegistry != null ? meterRegistry : manager.getMeterRegistry())
                : null;
        TieredCacheImpl<K, V> cache = new TieredCacheImpl<>(
                name, l1MaxSize, l1RefreshAfterWrite, l2KeyPrefix, l2Ttl, l2TtlJitter,
                keySchemaVersion, nullValueTtl, hotspotProtection, loaderFailure,
                redisReadFailure, redisWriteFailure, deserializeFailure, loader,
                valueJavaType, objectMapper, manager.getRedis(), manager.getReactiveRedis(), registry);
        manager.register(cache);
        return cache;
    }
}
