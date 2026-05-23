package org.chovy.canvas.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.chovy.cache.TieredCache;
import org.chovy.cache.TieredCacheManager;
import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.canvas.CanvasVersionMapper;
import org.chovy.canvas.infra.cache.CanvasEntityCache;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CacheConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CacheConfig.class, CanvasEntityCache.class)
            .withBean(CanvasMapper.class, () -> mock(CanvasMapper.class))
            .withBean(CanvasVersionMapper.class, () -> mock(CanvasVersionMapper.class))
            .withBean(TieredCacheManager.class, () -> new TieredCacheManager(
                    mock(StringRedisTemplate.class),
                    mock(ReactiveStringRedisTemplate.class),
                    new SimpleMeterRegistry(),
                    null));

    @Test
    void cacheConfigurationKeepsWrapperBeanSeparateFromTieredCacheBean() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(CanvasEntityCache.class);
            assertThat(context).hasBean("canvasEntityTieredCache");
            assertThat(context.getBean("canvasEntityTieredCache")).isInstanceOf(TieredCache.class);
            assertThat(context.getBean("canvasEntityCache")).isInstanceOf(CanvasEntityCache.class);

            @SuppressWarnings("unchecked")
            TieredCache<Long, Canvas> cache = context.getBean("canvasEntityTieredCache", TieredCache.class);
            assertThat(cache.name()).isEqualTo("canvas-entity");
        });
    }
}
