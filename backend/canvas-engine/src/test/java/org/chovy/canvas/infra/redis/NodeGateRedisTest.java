package org.chovy.canvas.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.scheduler.TraceWriteBuffer;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NodeGateRedisTest {

    @Test
    void gateKeyUsesExecutionAndNodeId() {
        RedisKeyUtil keys = new RedisKeyUtil();

        assertThat(keys.gate("exec-1", "node-1"))
                .isEqualTo("canvas:gate:exec-1:node-1");
    }

    @Test
    void tryAcquireNodeGate_firstAcquirerWinsAndSecondLoses() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), any(List.class), anyString(), anyString()))
                .thenReturn(1L)
                .thenReturn(0L);
        ContextPersistenceService service = service(redis);

        assertThat(service.tryAcquireNodeGate("exec-1", "node-1", "owner-1", Duration.ofSeconds(30))).isTrue();
        assertThat(service.tryAcquireNodeGate("exec-1", "node-1", "owner-2", Duration.ofSeconds(30))).isFalse();
        verify(redis).execute(any(RedisScript.class),
                eq(List.of("canvas:gate:exec-1:node-1", "canvas:gate-repeat:exec-1:node-1")),
                eq("owner-1"),
                eq("30000"));
        verify(redis).execute(any(RedisScript.class),
                eq(List.of("canvas:gate:exec-1:node-1", "canvas:gate-repeat:exec-1:node-1")),
                eq("owner-2"),
                eq("30000"));
    }

    @Test
    void releaseNodeGateDeletesOnlyOwnedGateByLua() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ContextPersistenceService service = service(redis);

        assertThat(service.releaseNodeGate("exec-1", "node-1", "owner-1")).isFalse();

        verify(redis).execute(any(RedisScript.class),
                eq(List.of("canvas:gate:exec-1:node-1", "canvas:gate-repeat:exec-1:node-1")),
                eq("owner-1"));
        verify(redis, never()).delete("canvas:gate:exec-1:node-1");
    }

    @Test
    void releaseNodeGateReturnsTrueWhenRepeatSignalConsumed() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), any(List.class), anyString())).thenReturn(1L);
        ContextPersistenceService service = service(redis);

        assertThat(service.releaseNodeGate("exec-1", "node-1", "owner-1")).isTrue();

        verify(redis).execute(any(RedisScript.class),
                eq(List.of("canvas:gate:exec-1:node-1", "canvas:gate-repeat:exec-1:node-1")),
                eq("owner-1"));
    }

    @Test
    void tryAcquireNodeGateFailsClosedWhenRedisThrows() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), any(List.class), anyString(), anyString()))
                .thenThrow(new RedisConnectionFailureException("redis down"));
        ContextPersistenceService service = service(redis);

        assertThat(service.tryAcquireNodeGate("exec-1", "node-1", "owner-1", Duration.ofSeconds(30))).isFalse();
    }

    @Test
    void releaseNodeGateLogsOnlyWhenRedisThrows() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        doThrow(new RedisConnectionFailureException("redis down"))
                .when(redis).execute(any(RedisScript.class), any(List.class), anyString());
        ContextPersistenceService service = service(redis);

        assertThat(service.releaseNodeGate("exec-1", "node-1", "owner-1")).isFalse();

        verify(redis).execute(any(RedisScript.class),
                eq(List.of("canvas:gate:exec-1:node-1", "canvas:gate-repeat:exec-1:node-1")),
                eq("owner-1"));
    }

    private ContextPersistenceService service(StringRedisTemplate redis) {
        return new ContextPersistenceService(redis, new ObjectMapper(), new RedisKeyUtil(),
                mock(TraceWriteBuffer.class));
    }
}
