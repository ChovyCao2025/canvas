package org.chovy.canvas.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.scheduler.TraceWriteBuffer;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent("canvas:gate:exec-1:node-1", "1", Duration.ofSeconds(30)))
                .thenReturn(true)
                .thenReturn(false);
        ContextPersistenceService service = service(redis);

        assertThat(service.tryAcquireNodeGate("exec-1", "node-1", Duration.ofSeconds(30))).isTrue();
        assertThat(service.tryAcquireNodeGate("exec-1", "node-1", Duration.ofSeconds(30))).isFalse();
    }

    @Test
    void releaseNodeGateDeletesGateKey() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ContextPersistenceService service = service(redis);

        service.releaseNodeGate("exec-1", "node-1");

        verify(redis).delete("canvas:gate:exec-1:node-1");
    }

    @Test
    void tryAcquireNodeGateFailsClosedWhenRedisThrows() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("redis down"));
        ContextPersistenceService service = service(redis);

        assertThat(service.tryAcquireNodeGate("exec-1", "node-1", Duration.ofSeconds(30))).isFalse();
    }

    @Test
    void releaseNodeGateLogsOnlyWhenRedisThrows() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        doThrow(new RedisConnectionFailureException("redis down")).when(redis).delete(any(String.class));
        ContextPersistenceService service = service(redis);

        service.releaseNodeGate("exec-1", "node-1");

        verify(redis).delete(eq("canvas:gate:exec-1:node-1"));
    }

    private ContextPersistenceService service(StringRedisTemplate redis) {
        return new ContextPersistenceService(redis, new ObjectMapper(), new RedisKeyUtil(),
                mock(TraceWriteBuffer.class));
    }
}
