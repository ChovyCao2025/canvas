package org.chovy.cache.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.cache.TieredCache;
import org.chovy.cache.TieredCacheBuilder;
import org.chovy.cache.TieredCacheManager;
import org.chovy.cache.annotation.TieredCached;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注解缓存解析器，负责把声明式缓存注解中的 cacheName 解析为具体 TieredCache 实例。
 *
 * <p>切面通过该组件隔离缓存查找逻辑，避免在 AOP 主流程中散落缓存注册中心访问代码。
 * <p>当缓存不存在或类型不匹配时，该类负责给出统一的错误边界。
 */
public class AnnotationCacheResolver {
    /** 缓存注册中心，用于查找或注册注解声明的缓存实例。 */
    private final TieredCacheManager manager;
    /** 注解缓存值序列化与反序列化使用的 ObjectMapper。 */
    private final ObjectMapper objectMapper;
    /** 按注解缓存名称缓存已解析的缓存实例。 */
    private final Map<String, TieredCache<Object, Object>> caches = new ConcurrentHashMap<>();

    /**
     * 创建注解缓存解析器。
     *
     * <p>解析器复用应用级 ObjectMapper 构建缓存值 JavaType，保证注解缓存与业务 JSON 配置一致。
     *
     * @param manager 缓存注册中心
     * @param objectMapper 注解缓存使用的序列化组件
     */
    public AnnotationCacheResolver(TieredCacheManager manager, ObjectMapper objectMapper) {
        this.manager = manager;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据 {@link TieredCached} 注解解析或创建缓存实例。
     *
     * <p>创建出的缓存没有固定 L3 loader，未命中加载由切面通过原方法返回值提供，因此直接调用默认 loader 会失败。
     *
     * @param annotation 声明式缓存读取注解
     * @return 可供切面读写的缓存实例
     */
    public TieredCache<Object, Object> resolve(TieredCached annotation) {
        return caches.computeIfAbsent(annotation.name(), ignored ->
                // 注解缓存没有固定 loader，真正的数据加载由切面在未命中时用原方法结果回填。
                TieredCacheBuilder.<Object, Object>builder()
                        .name(annotation.name())
                        .l1MaxSize(annotation.l1MaxSize())
                        .l1RefreshAfterWrite(parseDuration(annotation.l1RefreshAfterWrite()))
                        .l2KeyPrefix(annotation.l2KeyPrefix())
                        .l2Ttl(parseDuration(annotation.l2Ttl()))
                        .l2TtlJitter(annotation.l2TtlJitter())
                        .keySchemaVersion(annotation.keySchemaVersion())
                        .nullValueTtl(parseDuration(annotation.nullValueTtl()))
                        .emptyValueTtl(parseDuration(annotation.emptyValueTtl()))
                        .lockTtl(parseDuration(annotation.lockTtl()))
                        .refreshAhead(parseDuration(annotation.refreshAhead()))
                        .staleTtl(parseDuration(annotation.staleTtl()))
                        .hotspotProtection(annotation.hotspotProtection())
                        .penetration(annotation.penetration())
                        .breakdown(annotation.breakdown())
                        .avalanche(annotation.avalanche())
                        .onLoaderFailure(annotation.onLoaderFailure())
                        .onRedisReadFailure(annotation.onRedisReadFailure())
                        .onRedisWriteFailure(annotation.onRedisWriteFailure())
                        .onDeserializeFailure(annotation.onDeserializeFailure())
                        .loader(key -> { throw new IllegalStateException("Annotation cache requires loaderOverride"); })
                        .objectMapper(objectMapper)
                        .valueType(annotation.valueType())
                        .build(manager));
    }

    /**
     * 查找已存在的注解缓存或手工注册缓存。
     *
     * <p>{@code @TieredCachePut} 和 {@code @TieredCacheEvict} 不负责创建缓存，只能操作已经解析或注册过的缓存。
     *
     * @param name 缓存实例名称
     * @return 已存在的缓存实例
     * @throws IllegalStateException 缓存名称未注册时抛出
     */
    @SuppressWarnings("unchecked")
    public TieredCache<Object, Object> getExisting(String name) {
        TieredCache<Object, Object> cache = caches.get(name);
        if (cache != null) {
            return cache;
        }
        // 允许业务代码手工构建并注册缓存，注解写入/失效仍可通过 manager 找到它。
        return (TieredCache<Object, Object>) manager.getCache(name)
                .orElseThrow(() -> new IllegalStateException("Unknown tiered cache: " + name));
    }

    /**
     * 在缓存存在时失效指定 key。
     *
     * <p>该方法允许失效注解在缓存尚未创建时静默跳过，避免只写路径强制初始化读取缓存。
     *
     * @param name 缓存实例名称
     * @param key 需要失效的业务缓存 key
     */
    @SuppressWarnings("unchecked")
    public void evictIfPresent(String name, Object key) {
        TieredCache<Object, Object> cache = caches.get(name);
        if (cache == null) {
            cache = (TieredCache<Object, Object>) manager.getCache(name).orElse(null);
        }
        if (cache != null) {
            cache.invalidate(key);
        }
    }

    /**
     * 解析注解中的 Duration 字符串。
     *
     * <p>支持 ISO-8601 Duration，以及 ms/s/m/h/d 后缀的简写；无后缀时按毫秒处理。
     *
     * @param value 注解中的时间配置字符串
     * @return 解析后的 Duration
     */
    private Duration parseDuration(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("p")) {
            // 以 P 开头时按 ISO-8601 Duration 解析，兼容 PT30S/PT1H 等标准写法。
            return Duration.parse(value);
        }
        if (normalized.endsWith("ms")) {
            return Duration.ofMillis(Long.parseLong(normalized.substring(0, normalized.length() - 2)));
        }
        if (normalized.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(normalized.substring(0, normalized.length() - 1)));
        }
        if (normalized.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(normalized.substring(0, normalized.length() - 1)));
        }
        if (normalized.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(normalized.substring(0, normalized.length() - 1)));
        }
        if (normalized.endsWith("d")) {
            return Duration.ofDays(Long.parseLong(normalized.substring(0, normalized.length() - 1)));
        }
        return Duration.ofMillis(Long.parseLong(normalized));
    }
}
