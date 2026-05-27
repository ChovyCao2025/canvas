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
     * 创建并注册 tiered Cache Manager 相关的 Spring Bean。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param redis redis 方法执行所需的业务参数
     * @param reactiveRedis reactiveRedis 方法执行所需的业务参数
     * @param reactiveFactory reactiveFactory 方法执行所需的业务参数
     * @param meterRegistry meterRegistry 方法执行所需的业务参数
     * @param invalidationPublishers invalidationPublishers 方法执行所需的业务参数
     * @return 方法执行后的业务结果
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
     * 创建并注册 spel Key Evaluator 相关的 Spring Bean。
     *
     * <p>该方法在应用启动时由 Spring 容器调用，用于装配运行依赖。
     *
     * @return 方法执行后的业务结果
     */
    @Bean
    @ConditionalOnMissingBean
    public SpelKeyEvaluator spelKeyEvaluator() {
        return new SpelKeyEvaluator();
    }

    /**
     * 创建并注册 annotation Cache Resolver 相关的 Spring Bean。
     *
     * <p>该方法在应用启动时由 Spring 容器调用，用于装配运行依赖。
     *
     * @param manager manager 方法执行所需的业务参数
     * @param objectMapper objectMapper 方法执行所需的业务参数
     * @return 方法执行后的业务结果
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
     * 创建并注册 tiered Cache Aspect 相关的 Spring Bean。
     *
     * <p>该方法在应用启动时由 Spring 容器调用，用于装配运行依赖。
     *
     * @param resolver resolver 方法执行所需的业务参数
     * @param keyEvaluator keyEvaluator 对应的缓存键、配置键或业务键
     * @return 方法执行后的业务结果
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AnnotationCacheResolver.class)
    public TieredCacheAspect tieredCacheAspect(AnnotationCacheResolver resolver,
                                               SpelKeyEvaluator keyEvaluator) {
        return new TieredCacheAspect(resolver, keyEvaluator);
    }
}
