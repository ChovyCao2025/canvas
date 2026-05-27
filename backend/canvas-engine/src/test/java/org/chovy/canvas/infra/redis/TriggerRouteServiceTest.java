package org.chovy.canvas.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Trigger Route 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class TriggerRouteServiceTest {

    private StringRedisTemplate redis;
    private SetOperations<String, String> setOps;
    private ValueOperations<String, String> valueOps;
    private TriggerRouteService service;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        setOps = mock(SetOperations.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForSet()).thenReturn(setOps);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq("test:trigger:routes:mutation-lock"), any(), any(Duration.class))).thenReturn(true);

        RedisKeyUtil keys = new RedisKeyUtil();
        ReflectionTestUtils.setField(keys, "prefix", "test");
        service = new TriggerRouteService(redis, keys, mock(ReactiveRedisConnectionFactory.class));
    }

    @Test
    void getCanvasByMqTopicReadsRedisEachTimeToAvoidCrossInstanceStaleRoutes() {
        when(setOps.members("test:trigger:mq:order.paid")).thenReturn(Set.of("101"));

        assertThat(service.getCanvasByMqTopic("order.paid")).containsExactly("101");
        assertThat(service.getCanvasByMqTopic("order.paid")).containsExactly("101");

        verify(setOps, times(2)).members("test:trigger:mq:order.paid");

        when(setOps.members("test:trigger:mq:order.paid")).thenReturn(Set.of("101", "202"));
        service.registerMq(202L, "order.paid");

        assertThat(service.getCanvasByMqTopic("order.paid")).containsExactlyInAnyOrder("101", "202");
        verify(setOps, times(3)).members("test:trigger:mq:order.paid");
    }

    @Test
    void getCanvasByMqTopicFiltersNonNumericRouteIds() {
        when(setOps.members("test:trigger:mq:order.paid")).thenReturn(Set.of("101", "bad", "  ", "-1"));

        assertThat(service.getCanvasByMqTopic("order.paid")).containsExactly("101");
    }
}
