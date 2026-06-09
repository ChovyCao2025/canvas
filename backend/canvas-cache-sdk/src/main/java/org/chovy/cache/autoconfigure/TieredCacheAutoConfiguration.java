package org.chovy.cache.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.chovy.cache.CacheInvalidationPublisher;
import org.chovy.cache.TieredCacheManager;
import org.chovy.cache.aop.AnnotationCacheResolver;
import org.chovy.cache.aop.SpelKeyEvaluator;
import org.chovy.cache.aop.TieredCacheAspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 分层缓存 Spring Boot 自动配置。
 *
 * <p>负责在应用启动时注册 TieredCacheManager、AOP 切面和辅助组件，使业务模块引入 SDK 后即可使用注解缓存。
 * <p>自动配置只提供基础 Bean，具体缓存实例仍由业务配置或构建器创建。
 */
@AutoConfiguration(after = {RedisAutoConfiguration.class, RedisReactiveAutoConfiguration.class})
public class TieredCacheAutoConfiguration {
    /**
     * 创建分层缓存管理器 Bean。
     *
     * <p>只在存在 {@link StringRedisTemplate} 且业务未自定义 manager 时生效；响应式 Redis、指标和外部失效发布器均为可选依赖。
     *
     * @param redis 同步 Redis 模板，用于 L2 缓存读写
     * @param reactiveRedis 响应式 Redis 模板提供器
     * @param reactiveFactory 响应式 Redis 连接工厂提供器
     * @param meterRegistry 指标注册表提供器
     * @param invalidationPublishers 外部失效发布器提供器
     * @return 自动装配的缓存管理器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(StringRedisTemplate.class)
    public TieredCacheManager tieredCacheManager(StringRedisTemplate redis,
                                                 ObjectProvider<ReactiveStringRedisTemplate> reactiveRedis,
                                                 ObjectProvider<ReactiveRedisConnectionFactory> reactiveFactory,
                                                 ObjectProvider<MeterRegistry> meterRegistry,
                                                 ObjectProvider<CacheInvalidationPublisher> invalidationPublishers) {
        // 使用 ObjectProvider 保持响应式 Redis、指标和外部失效发布器可选，避免 SDK 强制应用引入全部依赖。
        return new TieredCacheManager(redis, reactiveRedis.getIfAvailable(),
                meterRegistry.getIfAvailable(), reactiveFactory.getIfAvailable(),
                invalidationPublishers.orderedStream().toList());
    }

    /**
     * 创建 SpEL key 计算器 Bean。
     *
     * <p>业务未自定义时提供默认实现，供缓存切面解析 key、condition 和 unless 表达式。
     *
     * @return SpEL key 计算器
     */
    @Bean
    @ConditionalOnMissingBean
    public SpelKeyEvaluator spelKeyEvaluator() {
        return new SpelKeyEvaluator();
    }

    /**
     * 创建注解缓存解析器 Bean。
     *
     * <p>解析器依赖缓存管理器，并优先复用应用已有 ObjectMapper 处理 L2 JSON 序列化。
     *
     * @param manager 缓存管理器
     * @param objectMapper ObjectMapper 提供器
     * @return 注解缓存解析器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(TieredCacheManager.class)
    public AnnotationCacheResolver annotationCacheResolver(TieredCacheManager manager,
                                                           ObjectProvider<ObjectMapper> objectMapper) {
        // 优先复用业务侧 ObjectMapper，确保注解缓存的序列化配置与应用 JSON 配置一致。
        return new AnnotationCacheResolver(manager, objectMapper.getIfAvailable(ObjectMapper::new));
    }

    /**
     * 创建分层缓存 AOP 切面 Bean。
     *
     * <p>切面负责拦截 {@code @TieredCached}、{@code @TieredCachePut} 和 {@code @TieredCacheEvict} 并执行缓存读写。
     *
     * @param resolver 注解缓存解析器
     * @param keyEvaluator SpEL key 计算器
     * @return 分层缓存切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AnnotationCacheResolver.class)
    public TieredCacheAspect tieredCacheAspect(AnnotationCacheResolver resolver,
                                               SpelKeyEvaluator keyEvaluator) {
        return new TieredCacheAspect(resolver, keyEvaluator);
    }
}
