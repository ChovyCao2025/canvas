package org.chovy.canvas.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.chovy.cache.TieredCache;
import org.chovy.cache.TieredCacheManager;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.chovy.canvas.infrastructure.cache.CanvasEntityCache;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Cache 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
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
            TieredCache<Long, CanvasDO> cache = context.getBean("canvasEntityTieredCache", TieredCache.class);
            assertThat(cache.name()).isEqualTo("canvas-entity");
        });
    }
}
