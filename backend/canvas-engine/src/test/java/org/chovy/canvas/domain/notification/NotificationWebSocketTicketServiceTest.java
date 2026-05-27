package org.chovy.canvas.domain.notification;

import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通知 WebSocket 票据 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class NotificationWebSocketTicketServiceTest {

    @Test
    void createTicketStoresShortLivedSingleUseTicket() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        RedisKeyUtil keys = new RedisKeyUtil();
        NotificationWebSocketTicketService service = new NotificationWebSocketTicketService(redis, keys);

        String ticket = service.createTicket("alice");

        assertThat(ticket).startsWith("ntf_ws_");
        verify(values).set(eq("canvas:notification:ws-ticket:" + ticket), eq("alice"), eq(Duration.ofSeconds(60)));
    }

    @Test
    void consumeTicketDeletesAndReturnsUserId() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.getAndDelete(anyString())).thenReturn("alice");
        RedisKeyUtil keys = new RedisKeyUtil();
        NotificationWebSocketTicketService service = new NotificationWebSocketTicketService(redis, keys);

        String userId = service.consumeTicket("ticket_1");

        assertThat(userId).isEqualTo("alice");
        verify(values).getAndDelete("canvas:notification:ws-ticket:ticket_1");
    }

    @Test
    void consumeTicketRejectsBlankTicket() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        RedisKeyUtil keys = new RedisKeyUtil();
        NotificationWebSocketTicketService service = new NotificationWebSocketTicketService(redis, keys);

        assertThat(service.consumeTicket(" ")).isNull();
    }
}
