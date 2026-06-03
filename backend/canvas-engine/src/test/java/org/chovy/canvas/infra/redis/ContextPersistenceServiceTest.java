package org.chovy.canvas.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.scheduler.TraceWriteBuffer;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContextPersistenceServiceTest {

    @Test
    void saveStoresRawExecutionContextWithoutMaskingRuntimeValues() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        TraceWriteBuffer traceBuffer = mock(TraceWriteBuffer.class);
        when(redis.opsForValue()).thenReturn(values);
        ContextPersistenceService service = new ContextPersistenceService(
                redis, new ObjectMapper(), new RedisKeyUtil(), traceBuffer);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setCanvasId(10L);
        ctx.setUserId("user-1");
        ctx.putRuntimeContextValue("password", "secret-value");

        service.save(ctx);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(values).set(eq("canvas:10:user:user-1"), json.capture(), any(Duration.class));
        assertThat(json.getValue()).contains("secret-value");
        assertThat(json.getValue()).doesNotContain("***");
    }

    @Test
    void saveProducesCriticalContextSaveTrace() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        TraceWriteBuffer traceBuffer = mock(TraceWriteBuffer.class);
        when(redis.opsForValue()).thenReturn(values);
        ContextPersistenceService service = new ContextPersistenceService(
                redis, new ObjectMapper(), new RedisKeyUtil(), traceBuffer);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-save-1");
        ctx.setCanvasId(10L);
        ctx.setUserId("user-1");

        service.save(ctx);

        ArgumentCaptor<CanvasExecutionTraceDO> trace = ArgumentCaptor.forClass(CanvasExecutionTraceDO.class);
        verify(traceBuffer).addTrace(trace.capture(), eq(true));
        assertThat(trace.getValue().getExecutionId()).isEqualTo("exec-save-1");
        assertThat(trace.getValue().getNodeId()).isEqualTo("CONTEXT_SAVE");
        assertThat(trace.getValue().getNodeType()).isEqualTo("SYSTEM");
        assertThat(trace.getValue().getStatus()).isEqualTo(1);
        assertThat(trace.getValue().getStartedAt()).isNotNull();
    }

    @Test
    void loadProducesCriticalContextLoadTraceAfterSuccessfulDeserialize() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
        SetOperations<String, String> sets = mock(SetOperations.class);
        TraceWriteBuffer traceBuffer = mock(TraceWriteBuffer.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForHash()).thenReturn(hashes);
        when(redis.opsForSet()).thenReturn(sets);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-load-1");
        ctx.setCanvasId(10L);
        ctx.setUserId("user-1");
        ctx.putNodeOutput("node-1", Map.of("field", "value"));
        when(values.get("canvas:10:user:user-1")).thenReturn(objectMapper.writeValueAsString(ctx));
        when(sets.members("canvas:node-state-index:exec-load-1")).thenReturn(Set.of());
        ContextPersistenceService service = new ContextPersistenceService(
                redis, objectMapper, new RedisKeyUtil(), traceBuffer);

        ExecutionContext loaded = service.load(10L, "user-1");

        assertThat(loaded).isNotNull();
        assertThat(loaded.getFlatContext()).containsEntry("node-1.field", "value");
        ArgumentCaptor<CanvasExecutionTraceDO> trace = ArgumentCaptor.forClass(CanvasExecutionTraceDO.class);
        verify(traceBuffer).addTrace(trace.capture(), eq(true));
        assertThat(trace.getValue().getExecutionId()).isEqualTo("exec-load-1");
        assertThat(trace.getValue().getNodeId()).isEqualTo("CONTEXT_LOAD");
        assertThat(trace.getValue().getNodeType()).isEqualTo("SYSTEM");
        assertThat(trace.getValue().getStatus()).isEqualTo(1);
        assertThat(trace.getValue().getStartedAt()).isNotNull();
    }

    @Test
    void loadMergesIncrementalNodeStatesIntoRecoveredContext() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
        SetOperations<String, String> sets = mock(SetOperations.class);
        TraceWriteBuffer traceBuffer = mock(TraceWriteBuffer.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForHash()).thenReturn(hashes);
        when(redis.opsForSet()).thenReturn(sets);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-load-2");
        ctx.setCanvasId(10L);
        ctx.setUserId("user-1");
        ctx.setNodeStatus("coupon", NodeStatus.WAITING);
        when(values.get("canvas:10:user:user-1")).thenReturn(objectMapper.writeValueAsString(ctx));
        when(sets.members("canvas:node-state-index:exec-load-2")).thenReturn(Set.of("coupon", "reach"));
        when(hashes.entries("canvas:node-state:exec-load-2:coupon")).thenReturn(Map.of(
                "status", "SUCCESS",
                "output", "{\"couponId\":\"C-1\"}"));
        when(hashes.entries("canvas:node-state:exec-load-2:reach")).thenReturn(Map.of(
                "status", "SUCCESS",
                "output", "{\"channel\":\"sms\"}"));
        ContextPersistenceService service = new ContextPersistenceService(
                redis, objectMapper, new RedisKeyUtil(), traceBuffer);

        ExecutionContext loaded = service.load(10L, "user-1");

        assertThat(loaded.getNodeStatus("coupon")).isEqualTo(NodeStatus.SUCCESS);
        assertThat(loaded.getNodeStatus("reach")).isEqualTo(NodeStatus.SUCCESS);
        assertThat(loaded.getNodeOutput("coupon", "couponId")).isEqualTo("C-1");
        assertThat(loaded.getNodeOutput("reach", "channel")).isEqualTo("sms");
        assertThat(loaded.getFlatContext()).containsEntry("coupon.couponId", "C-1");
        verify(redis, never()).keys("canvas:node-state:exec-load-2:*");
    }

    @Test
    void loadFailsClosedWhenIncrementalNodeStateIndexReadFails() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        SetOperations<String, String> sets = mock(SetOperations.class);
        TraceWriteBuffer traceBuffer = mock(TraceWriteBuffer.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForSet()).thenReturn(sets);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-load-3");
        ctx.setCanvasId(10L);
        ctx.setUserId("user-1");
        when(values.get("canvas:10:user:user-1")).thenReturn(objectMapper.writeValueAsString(ctx));
        when(sets.members("canvas:node-state-index:exec-load-3"))
                .thenThrow(new RedisConnectionFailureException("redis down"));
        ContextPersistenceService service = new ContextPersistenceService(
                redis, objectMapper, new RedisKeyUtil(), traceBuffer);

        assertThatThrownBy(() -> service.load(10L, "user-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to merge incremental node states");
    }

    @Test
    void loadFailsClosedWhenIndexedNodeStateHashIsMissing() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
        SetOperations<String, String> sets = mock(SetOperations.class);
        TraceWriteBuffer traceBuffer = mock(TraceWriteBuffer.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForHash()).thenReturn(hashes);
        when(redis.opsForSet()).thenReturn(sets);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-load-4");
        ctx.setCanvasId(10L);
        ctx.setUserId("user-1");
        ctx.setNodeStatus("target", NodeStatus.SUCCESS);
        ctx.putNodeOutput("target", Map.of("stale", true));
        when(values.get("canvas:10:user:user-1")).thenReturn(objectMapper.writeValueAsString(ctx));
        when(sets.members("canvas:node-state-index:exec-load-4")).thenReturn(Set.of("target"));
        when(hashes.entries("canvas:node-state:exec-load-4:target")).thenReturn(Map.of());
        ContextPersistenceService service = new ContextPersistenceService(
                redis, objectMapper, new RedisKeyUtil(), traceBuffer);

        assertThatThrownBy(() -> service.load(10L, "user-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to merge incremental node states")
                .hasRootCauseMessage("Missing indexed node state: target");
    }

    @Test
    void loadFailsClosedWhenIndexedNodeStateOutputIsCorrupted() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
        SetOperations<String, String> sets = mock(SetOperations.class);
        TraceWriteBuffer traceBuffer = mock(TraceWriteBuffer.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForHash()).thenReturn(hashes);
        when(redis.opsForSet()).thenReturn(sets);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-load-6");
        ctx.setCanvasId(10L);
        ctx.setUserId("user-1");
        when(values.get("canvas:10:user:user-1")).thenReturn(objectMapper.writeValueAsString(ctx));
        when(sets.members("canvas:node-state-index:exec-load-6")).thenReturn(Set.of("target"));
        when(hashes.entries("canvas:node-state:exec-load-6:target")).thenReturn(Map.of(
                "status", "SUCCESS",
                "output", "{not-json"));
        ContextPersistenceService service = new ContextPersistenceService(
                redis, objectMapper, new RedisKeyUtil(), traceBuffer);

        assertThatThrownBy(() -> service.load(10L, "user-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to merge incremental node states")
                .hasCauseInstanceOf(IllegalStateException.class)
                .cause()
                .hasMessageContaining("Failed to parse node output from Redis");
    }

    @Test
    void loadFailsClosedWhenResetMarkerExistsEvenIfOldHashExists() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
        SetOperations<String, String> sets = mock(SetOperations.class);
        TraceWriteBuffer traceBuffer = mock(TraceWriteBuffer.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForHash()).thenReturn(hashes);
        when(redis.opsForSet()).thenReturn(sets);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-load-5");
        ctx.setCanvasId(10L);
        ctx.setUserId("user-1");
        ctx.setNodeStatus("target", NodeStatus.SUCCESS);
        ctx.putNodeOutput("target", Map.of("stale", true));
        when(values.get("canvas:10:user:user-1")).thenReturn(objectMapper.writeValueAsString(ctx));
        when(sets.members("canvas:node-state-reset:exec-load-5")).thenReturn(Set.of("target"));
        when(sets.members("canvas:node-state-index:exec-load-5")).thenReturn(Set.of("target"));
        when(hashes.entries("canvas:node-state:exec-load-5:target")).thenReturn(Map.of(
                "status", "SUCCESS",
                "output", "{\"stale\":true}"));
        ContextPersistenceService service = new ContextPersistenceService(
                redis, objectMapper, new RedisKeyUtil(), traceBuffer);

        assertThatThrownBy(() -> service.load(10L, "user-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to merge incremental node states")
                .hasRootCauseMessage("Pending node state reset markers: [target]");
    }

    @Test
    void saveStillPersistsWhenCriticalTraceThrows() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        TraceWriteBuffer traceBuffer = mock(TraceWriteBuffer.class);
        when(redis.opsForValue()).thenReturn(values);
        doThrow(new IllegalStateException("trace buffer full"))
                .when(traceBuffer).addTrace(any(CanvasExecutionTraceDO.class), eq(true));
        ContextPersistenceService service = new ContextPersistenceService(
                redis, new ObjectMapper(), new RedisKeyUtil(), traceBuffer);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-save-2");
        ctx.setCanvasId(10L);
        ctx.setUserId("user-1");

        service.save(ctx);

        verify(values).set(eq("canvas:10:user:user-1"), any(String.class), any(Duration.class));
        verify(traceBuffer).addTrace(any(CanvasExecutionTraceDO.class), eq(true));
    }
}
