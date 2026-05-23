package org.chovy.cache.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.chovy.cache.TieredCacheManager;
import org.chovy.cache.aop.AnnotationCacheResolver;
import org.chovy.cache.aop.SpelKeyEvaluator;
import org.chovy.cache.aop.TieredCacheAspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
public class TieredCacheAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(StringRedisTemplate.class)
    public TieredCacheManager tieredCacheManager(StringRedisTemplate redis,
                                                 ObjectProvider<ReactiveStringRedisTemplate> reactiveRedis,
                                                 ObjectProvider<ReactiveRedisConnectionFactory> reactiveFactory,
                                                 ObjectProvider<MeterRegistry> meterRegistry) {
        return new TieredCacheManager(redis, reactiveRedis.getIfAvailable(),
                meterRegistry.getIfAvailable(), reactiveFactory.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public SpelKeyEvaluator spelKeyEvaluator() {
        return new SpelKeyEvaluator();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(TieredCacheManager.class)
    public AnnotationCacheResolver annotationCacheResolver(TieredCacheManager manager,
                                                           ObjectProvider<ObjectMapper> objectMapper) {
        return new AnnotationCacheResolver(manager, objectMapper.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AnnotationCacheResolver.class)
    public TieredCacheAspect tieredCacheAspect(AnnotationCacheResolver resolver,
                                               SpelKeyEvaluator keyEvaluator) {
        return new TieredCacheAspect(resolver, keyEvaluator);
    }
}
