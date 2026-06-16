package org.chovy.cache.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.cache.TieredCache;
import org.chovy.cache.TieredCacheManager;
import org.chovy.cache.annotation.TieredCached;
import org.chovy.cache.aop.AnnotationCacheResolver;
import org.chovy.cache.aop.SpelKeyEvaluator;
import org.chovy.cache.aop.TieredCacheAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TestTieredCacheConfig 是分层缓存 SDK 的测试支撑组件。
 *
 * <p>用于在业务或单元测试中替代真实 Redis/多级缓存依赖，使缓存调用可以在内存中稳定验证。
 * <p>该组件只服务测试场景，不应承载生产缓存策略。
 */
@Configuration
public class TestTieredCacheConfig {
    /**
     * 创建测试专用注解缓存解析器。
     *
     * <p>解析器使用 {@link InMemoryTieredCache} 替代 Redis 分层缓存，让业务测试不依赖外部 Redis 服务。
     *
     * @return 测试注解缓存解析器
     */
    @Bean
    @Primary
    public AnnotationCacheResolver testAnnotationCacheResolver() {
        return new AnnotationCacheResolver(null, new ObjectMapper()) {
            /**
             * 测试环境中按缓存名称复用的内存缓存实例。
             */
            private final Map<String, TieredCache<Object, Object>> caches = new ConcurrentHashMap<>();

            /**
             * 解析或懒创建注解声明的内存缓存。
             *
             * <p>未命中加载由切面执行原方法后写入，因此这里的默认 loader 只作为误用保护。
             *
             * @param annotation 缓存读取注解
             * @return 测试内存缓存实例
             */
            @Override
            public TieredCache<Object, Object> resolve(TieredCached annotation) {
                return caches.computeIfAbsent(annotation.name(), name ->
                        // 测试注解缓存不访问 Redis，未命中数据仍由切面执行原方法后写入内存缓存。
                        InMemoryTieredCache.of(name, key -> {
                            throw new IllegalStateException("Annotation test cache requires loaderOverride");
                        }));
            }

            /**
             * 获取已存在或懒创建的测试缓存。
             *
             * <p>允许 {@code @TieredCachePut} 先于 {@code @TieredCached} 使用同名缓存，贴近业务测试写路径。
             *
             * @param name 缓存实例名称
             * @return 测试内存缓存实例
             */
            @Override
            public TieredCache<Object, Object> getExisting(String name) {
                // @TieredCachePut 可先于 @TieredCached 出现，因此按名称懒创建同一份内存缓存。
                return caches.computeIfAbsent(name, cacheName ->
                        InMemoryTieredCache.of(cacheName, key -> null));
            }

            /**
             * 在测试缓存存在时失效指定 key。
             *
             * <p>缓存尚未创建时静默跳过，避免失效注解强制初始化无关缓存。
             *
             * @param name 缓存实例名称
             * @param key 需要失效的业务缓存 key
             */
            @Override
            public void evictIfPresent(String name, Object key) {
                TieredCache<Object, Object> cache = caches.get(name);
                if (cache != null) {
                    cache.invalidate(key);
                }
            }
        };
    }

    /**
     * 创建测试用 SpEL key 计算器。
     *
     * <p>保持与生产切面相同的 key、condition 和 unless 表达式解析语义。
     *
     * @return SpEL key 计算器
     */
    @Bean
    @Primary
    public SpelKeyEvaluator testSpelKeyEvaluator() {
        return new SpelKeyEvaluator();
    }

    /**
     * 创建测试用分层缓存切面。
     *
     * <p>切面逻辑与生产一致，只把底层缓存替换为内存实现，便于验证注解缓存行为。
     *
     * @param resolver 测试注解缓存解析器
     * @param keyEvaluator SpEL key 计算器
     * @return 测试缓存切面
     */
    @Bean
    @Primary
    public TieredCacheAspect testTieredCacheAspect(AnnotationCacheResolver resolver,
                                                   SpelKeyEvaluator keyEvaluator) {
        return new TieredCacheAspect(resolver, keyEvaluator);
    }

    /**
     * 创建测试用缓存管理器。
     *
     * <p>测试管理器不连接 Redis，也不启动 Pub/Sub；仅用于满足依赖注入和手工注册场景。
     *
     * @return 测试缓存管理器
     */
    @Bean
    @Primary
    public TieredCacheManager testTieredCacheManager() {
        return new TieredCacheManager(null, null, null, null);
    }
}
