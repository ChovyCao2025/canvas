package org.chovy.canvas.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.scheduler.TraceWriteBuffer;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContextPersistenceIncrementalTest {

    @Test
    void saveNodeState_writesStatusAndOutputToRedisHash() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashes);
        when(redis.expire("canvas:node-state:exec-1:node-1", Duration.ofSeconds(123))).thenReturn(true);
        ContextPersistenceService service = service(redis, objectMapper);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("amount", 42);
        output.put("ok", true);

        service.saveNodeState("exec-1", "node-1", NodeStatus.SUCCESS, output);

        ArgumentCaptor<Map<String, String>> fields = ArgumentCaptor.captor();
        verify(hashes).putAll(eq("canvas:node-state:exec-1:node-1"), fields.capture());
        assertThat(fields.getValue()).containsEntry("status", "SUCCESS");
        assertThat(objectMapper.readValue(fields.getValue().get("output"), Map.class))
                .containsEntry("amount", 42)
                .containsEntry("ok", true);
        verify(redis).expire("canvas:node-state:exec-1:node-1", Duration.ofSeconds(123));
    }

    @Test
    void saveNodeState_deletesHashWhenTtlRefreshFails() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashes);
        when(redis.expire("canvas:node-state:exec-1:node-1", Duration.ofSeconds(123))).thenReturn(false);
        ContextPersistenceService service = service(redis);

        service.saveNodeState("exec-1", "node-1", NodeStatus.SUCCESS, Map.of());

        verify(hashes).putAll(eq("canvas:node-state:exec-1:node-1"), anyMap());
        verify(redis).delete("canvas:node-state:exec-1:node-1");
    }

    @Test
    void deleteNodeState_deletesRedisKey() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ContextPersistenceService service = service(redis);

        service.deleteNodeState("exec-1", "node-1");

        verify(redis).delete("canvas:node-state:exec-1:node-1");
    }

    @Test
    void loadNodeState_readsFromRedisHash() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashes);
        when(hashes.entries("canvas:node-state:exec-1:node-1")).thenReturn(Map.of(
                "status", "SUCCESS",
                "output", "{\"amount\":42,\"ok\":true}"
        ));
        ContextPersistenceService service = service(redis);

        ContextPersistenceService.NodeState loaded = service.loadNodeState("exec-1", "node-1");

        assertThat(loaded.status()).isEqualTo(NodeStatus.SUCCESS);
        assertThat(loaded.output()).containsEntry("amount", 42).containsEntry("ok", true);
    }

    @Test
    void loadNodeState_returnsNullWhenMissing() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashes);
        when(hashes.entries("canvas:node-state:exec-1:missing")).thenReturn(Map.of());
        ContextPersistenceService service = service(redis);

        assertThat(service.loadNodeState("exec-1", "missing")).isNull();
    }

    @Test
    void loadNodeState_returnsEmptyOutputWhenOutputJsonInvalid() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashes);
        when(hashes.entries("canvas:node-state:exec-1:node-1")).thenReturn(Map.of(
                "status", "FAILED",
                "output", "{not-json"
        ));
        ContextPersistenceService service = service(redis);

        ContextPersistenceService.NodeState loaded = service.loadNodeState("exec-1", "node-1");

        assertThat(loaded.status()).isEqualTo(NodeStatus.FAILED);
        assertThat(loaded.output()).isEmpty();
    }

    private ContextPersistenceService service(StringRedisTemplate redis) {
        return service(redis, new ObjectMapper());
    }

    private ContextPersistenceService service(StringRedisTemplate redis, ObjectMapper objectMapper) {
        ContextPersistenceService service = new ContextPersistenceService(
                redis, objectMapper, new RedisKeyUtil(), mock(TraceWriteBuffer.class));
        ReflectionTestUtils.setField(service, "ttlSec", 123L);
        return service;
    }
}
