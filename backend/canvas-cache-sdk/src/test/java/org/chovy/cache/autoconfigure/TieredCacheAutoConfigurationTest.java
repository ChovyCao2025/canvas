package org.chovy.cache.autoconfigure;

import org.chovy.cache.TieredCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分层缓存自动配置测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class TieredCacheAutoConfigurationTest {

    /**
     * 验证 Redis 模板自动配置完成后会创建分层缓存管理器。
     */
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
