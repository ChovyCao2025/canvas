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
     * 创建并注册 test Annotation Cache Resolver 相关的 Spring Bean。
     *
     * <p>该方法在应用启动时由 Spring 容器调用，用于装配运行依赖。
     *
     * @return 方法执行后的业务结果
     */
    @Bean
    @Primary
    public AnnotationCacheResolver testAnnotationCacheResolver() {
        return new AnnotationCacheResolver(null, new ObjectMapper()) {
            /** 测试环境中按缓存名称复用的内存缓存实例。 */
            private final Map<String, TieredCache<Object, Object>> caches = new ConcurrentHashMap<>();

            /**
             * 构建、解析或转换 resolve 相关的业务数据。
             *
             * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
             *
             * @param annotation annotation 方法执行所需的业务参数
             * @return 方法执行后的业务结果
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
             * 查询或读取 get Existing 相关的业务数据。
             *
             * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
             *
             * @param name name 方法执行所需的业务参数
             * @return 方法执行后的业务结果
             */
            @Override
            public TieredCache<Object, Object> getExisting(String name) {
                // @TieredCachePut 可先于 @TieredCached 出现，因此按名称懒创建同一份内存缓存。
                return caches.computeIfAbsent(name, cacheName ->
                        InMemoryTieredCache.of(cacheName, key -> null));
            }

            /**
             * 删除、清理或失效 evict If Present 相关的业务数据。
             *
             * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
             *
             * @param name name 方法执行所需的业务参数
             * @param key key 对应的缓存键、配置键或业务键
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
     * 创建并注册 test Spel Key Evaluator 相关的 Spring Bean。
     *
     * <p>该方法在应用启动时由 Spring 容器调用，用于装配运行依赖。
     *
     * @return 方法执行后的业务结果
     */
    @Bean
    @Primary
    public SpelKeyEvaluator testSpelKeyEvaluator() {
        return new SpelKeyEvaluator();
    }

    /**
     * 创建并注册 test Tiered Cache Aspect 相关的 Spring Bean。
     *
     * <p>该方法在应用启动时由 Spring 容器调用，用于装配运行依赖。
     *
     * @param resolver resolver 方法执行所需的业务参数
     * @param keyEvaluator keyEvaluator 对应的缓存键、配置键或业务键
     * @return 方法执行后的业务结果
     */
    @Bean
    @Primary
    public TieredCacheAspect testTieredCacheAspect(AnnotationCacheResolver resolver,
                                                   SpelKeyEvaluator keyEvaluator) {
        return new TieredCacheAspect(resolver, keyEvaluator);
    }

    /**
     * 创建并注册 test Tiered Cache Manager 相关的 Spring Bean。
     *
     * <p>该方法在应用启动时由 Spring 容器调用，用于装配运行依赖。
     *
     * @return 方法执行后的业务结果
     */
    @Bean
    @Primary
    public TieredCacheManager testTieredCacheManager() {
        return new TieredCacheManager(null, null, null, null);
    }
}
