package org.chovy.canvas.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.scheduler.TraceWriteBuffer;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
        TraceWriteBuffer traceBuffer = mock(TraceWriteBuffer.class);
        when(redis.opsForValue()).thenReturn(values);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-load-1");
        ctx.setCanvasId(10L);
        ctx.setUserId("user-1");
        ctx.putNodeOutput("node-1", Map.of("field", "value"));
        when(values.get("canvas:10:user:user-1")).thenReturn(objectMapper.writeValueAsString(ctx));
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
