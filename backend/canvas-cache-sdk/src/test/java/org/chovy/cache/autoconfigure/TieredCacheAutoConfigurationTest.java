package org.chovy.cache.autoconfigure;

import org.chovy.cache.TieredCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class TieredCacheAutoConfigurationTest {

    @Test
    void createsCacheManagerAfterRedisTemplatesAreAutoConfigured() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        TieredCacheAutoConfiguration.class,
                        RedisAutoConfiguration.class,
                        RedisReactiveAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(StringRedisTemplate.class);
                    assertThat(context).hasSingleBean(TieredCacheManager.class);
                });
    }
}
