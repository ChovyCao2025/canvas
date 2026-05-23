package org.chovy.cache.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import org.chovy.cache.TieredCacheManager;
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
}
