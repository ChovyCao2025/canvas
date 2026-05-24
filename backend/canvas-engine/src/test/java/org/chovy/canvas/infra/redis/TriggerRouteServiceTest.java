package org.chovy.canvas.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TriggerRouteServiceTest {

    private StringRedisTemplate redis;
    private SetOperations<String, String> setOps;
    private TriggerRouteService service;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        setOps = mock(SetOperations.class);
        when(redis.opsForSet()).thenReturn(setOps);

        RedisKeyUtil keys = new RedisKeyUtil();
        ReflectionTestUtils.setField(keys, "prefix", "test");
        service = new TriggerRouteService(redis, keys, mock(ReactiveRedisConnectionFactory.class));
    }

    @Test
    void getCanvasByMqTopicCachesRedisSetUntilRouteChanges() {
        when(setOps.members("test:trigger:mq:order.paid")).thenReturn(Set.of("101"));

        assertThat(service.getCanvasByMqTopic("order.paid")).containsExactly("101");
        assertThat(service.getCanvasByMqTopic("order.paid")).containsExactly("101");

        verify(setOps, times(1)).members("test:trigger:mq:order.paid");

        when(setOps.members("test:trigger:mq:order.paid")).thenReturn(Set.of("101", "202"));
        service.registerMq(202L, "order.paid");

        assertThat(service.getCanvasByMqTopic("order.paid")).containsExactlyInAnyOrder("101", "202");
        verify(setOps, times(2)).members("test:trigger:mq:order.paid");
    }

    @Test
    void getCanvasByMqTopicFiltersNonNumericRouteIds() {
        when(setOps.members("test:trigger:mq:order.paid")).thenReturn(Set.of("101", "bad", "  ", "-1"));

        assertThat(service.getCanvasByMqTopic("order.paid")).containsExactly("101");
    }
}
