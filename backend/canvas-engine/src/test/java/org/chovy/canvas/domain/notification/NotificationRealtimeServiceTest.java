package org.chovy.canvas.domain.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.chovy.canvas.dto.notification.NotificationRealtimePayload;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationRealtimeServiceTest {

    @Test
    void rejectsConnectionsAbovePerUserLimit() {
        NotificationRealtimeService service = service(1, 10);
        WebSocketSession first = session("session-1");
        WebSocketSession second = session("session-2");
        when(second.close(CloseStatus.POLICY_VIOLATION)).thenReturn(Mono.empty());

        service.register("alice", first, NotificationRealtimePayload.sync(null, 0));

        StepVerifier.create(service.register("alice", second, NotificationRealtimePayload.sync(null, 0)))
                .verifyComplete();

        assertThat(service.activeSessionCount("alice")).isEqualTo(1);
        verify(second).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void rejectsConnectionsAboveGlobalLimit() {
        NotificationRealtimeService service = service(5, 1);
        WebSocketSession first = session("session-1");
        WebSocketSession second = session("session-2");
        when(second.close(CloseStatus.POLICY_VIOLATION)).thenReturn(Mono.empty());

        service.register("alice", first, NotificationRealtimePayload.sync(null, 0));

        StepVerifier.create(service.register("bob", second, NotificationRealtimePayload.sync(null, 0)))
                .verifyComplete();

        assertThat(service.activeSessionCount("alice")).isEqualTo(1);
        assertThat(service.activeSessionCount("bob")).isZero();
        assertThat(service.totalActiveSessionCount()).isEqualTo(1);
        verify(second).close(CloseStatus.POLICY_VIOLATION);
    }

    private NotificationRealtimeService service(int maxSessionsPerUser, int maxTotalSessions) {
        return new NotificationRealtimeService(
                new ObjectMapper().registerModule(new JavaTimeModule()),
                mock(StringRedisTemplate.class),
                mock(ReactiveRedisConnectionFactory.class),
                mock(RedisKeyUtil.class),
                maxSessionsPerUser,
                maxTotalSessions);
    }

    private WebSocketSession session(String sessionId) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(sessionId);
        when(session.send(any())).thenReturn(Mono.never());
        when(session.receive()).thenReturn(Flux.never());
        return session;
    }
}
