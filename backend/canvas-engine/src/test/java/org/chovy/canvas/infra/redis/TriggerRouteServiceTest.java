package org.chovy.canvas.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    void registerMqUsesManagedWaiterBetweenRouteMutationLockAttempts() {
        AtomicInteger waitCalls = new AtomicInteger();
        RedisKeyUtil keys = new RedisKeyUtil();
        ReflectionTestUtils.setField(keys, "prefix", "test");
        TriggerRouteService routeService = new TriggerRouteService(
                redis,
                keys,
                mock(ReactiveRedisConnectionFactory.class),
                waitCalls::incrementAndGet);
        when(valueOps.setIfAbsent(eq("test:trigger:routes:mutation-lock"), any(), any(Duration.class)))
                .thenReturn(false, false, true);

        routeService.registerMq(202L, "order.paid");

        assertThat(waitCalls).hasValue(2);
        verify(setOps).add("test:trigger:mq:order.paid", "202");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void replaceTriggerRoutesReplacesMqBehaviorAndTaggerFamilies() {
        RedisOperations operations = mock(RedisOperations.class);
        SetOperations txSetOps = mock(SetOperations.class);
        when(operations.opsForSet()).thenReturn(txSetOps);
        when(operations.exec()).thenReturn(List.of());
        Cursor<String> mqCursor = cursor("test:trigger:mq:old");
        Cursor<String> behaviorCursor = cursor("test:trigger:behavior:old");
        Cursor<String> taggerCursor = cursor("test:trigger:tagger:old");
        when(redis.scan(any()))
                .thenReturn(mqCursor)
                .thenReturn(behaviorCursor)
                .thenReturn(taggerCursor);
        when(redis.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback callback = invocation.getArgument(0);
            return callback.execute(operations);
        });

        service.replaceTriggerRoutes(
                Map.of("order.paid", Set.of("10")),
                Map.of("ORDER_PAID", Set.of("10")),
                Map.of("vip_level", Set.of("10")));

        ArgumentCaptor<Collection> deletedKeys = ArgumentCaptor.forClass(Collection.class);
        verify(operations).delete(deletedKeys.capture());
        assertThat(deletedKeys.getValue()).containsExactlyInAnyOrder(
                "test:trigger:mq:old",
                "test:trigger:behavior:old",
                "test:trigger:tagger:old");
        verify(txSetOps).add("test:trigger:mq:order.paid", "10");
        verify(txSetOps).add("test:trigger:behavior:ORDER_PAID", "10");
        verify(txSetOps).add("test:trigger:tagger:vip_level", "10");
    }

    private static Cursor<String> cursor(String... values) {
        Cursor<String> cursor = mock(Cursor.class);
        AtomicInteger index = new AtomicInteger();
        when(cursor.hasNext()).thenAnswer(ignored -> index.get() < values.length);
        when(cursor.next()).thenAnswer(ignored -> values[index.getAndIncrement()]);
        return cursor;
    }
}
