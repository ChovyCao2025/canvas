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
     * 构造 AnnotationCacheResolver 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param manager manager 方法执行所需的业务参数
     * @param objectMapper objectMapper 方法执行所需的业务参数
     */
    public AnnotationCacheResolver(TieredCacheManager manager, ObjectMapper objectMapper) {
        this.manager = manager;
        this.objectMapper = objectMapper;
    }

    /**
     * 构建、解析或转换 resolve 相关的业务数据。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param annotation annotation 方法执行所需的业务参数
     * @return 方法执行后的业务结果
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
     * 查询或读取 get Existing 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param name name 方法执行所需的业务参数
     * @return 方法执行后的业务结果
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
     * 删除、清理或失效 evict If Present 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param name name 方法执行所需的业务参数
     * @param key key 对应的缓存键、配置键或业务键
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
     * 构建、解析或转换 parse Duration 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 方法执行后的业务结果
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
