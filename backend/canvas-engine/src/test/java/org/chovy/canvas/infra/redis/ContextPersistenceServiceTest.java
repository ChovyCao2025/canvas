package org.chovy.canvas.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;

class ContextPersistenceServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void saveStoresRawExecutionContextWithoutMaskingRuntimeValues() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
        ContextPersistenceService service = service(redis, executionMapper);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setCanvasId(10L);
        ctx.setUserId("user-1");
        ctx.putTriggerPayloadValues(java.util.Map.of("password", "secret-value"));

        service.save(ctx);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(values).set(eq("canvas:10:user:user-1"), json.capture(), any(Duration.class));
        assertThat(json.getValue()).contains("secret-value");
        assertThat(json.getValue()).doesNotContain("***");
        verify(executionMapper, never()).updateContextSnapshot(any(), any());
    }

    @Test
    void saveWritesExecutionContextColdBackupWhenExecutionIdExists() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
        ContextPersistenceService service = service(redis, executionMapper);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-1");
        ctx.setCanvasId(10L);
        ctx.setUserId("user-1");
        ctx.putTriggerPayloadValues(java.util.Map.of("approvalId", "approval-9"));

        service.save(ctx);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(executionMapper).updateContextSnapshot(eq("exec-1"), json.capture());
        assertThat(json.getValue()).contains("approval-9");
    }

    @Test
    void loadFallsBackToPausedExecutionSnapshotWhenRedisContextIsMissing() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
        ContextPersistenceService service = service(redis, executionMapper);
        ExecutionContext paused = new ExecutionContext();
        paused.setExecutionId("exec-paused");
        paused.setCanvasId(10L);
        paused.setUserId("user-1");
        paused.putTriggerPayloadValues(java.util.Map.of("waitNode", "wait-1"));
        CanvasExecutionDO backup = new CanvasExecutionDO();
        backup.setContextSnapshotJson(OBJECT_MAPPER.writeValueAsString(paused));
        when(values.get("canvas:10:user:user-1")).thenReturn(null);
        when(executionMapper.selectLatestPausedContextSnapshot(10L, "user-1"))
                .thenReturn(backup);

        ExecutionContext loaded = service.load(10L, "user-1");

        assertThat(loaded).isNotNull();
        assertThat(loaded.getExecutionId()).isEqualTo("exec-paused");
        assertThat(loaded.getTriggerPayload()).containsEntry("waitNode", "wait-1");
        verify(values).set(eq("canvas:10:user:user-1"), any(String.class), any(Duration.class));
    }

    @Test
    void existsReturnsTrueWhenPausedSnapshotExistsInDatabase() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
        ContextPersistenceService service = service(redis, executionMapper);
        CanvasExecutionDO backup = new CanvasExecutionDO();
        backup.setContextSnapshotJson("{\"executionId\":\"exec-paused\"}");
        when(redis.hasKey("canvas:10:user:user-1")).thenReturn(false);
        when(executionMapper.selectLatestPausedContextSnapshot(10L, "user-1"))
                .thenReturn(backup);

        assertThat(service.exists(10L, "user-1")).isTrue();
    }

    @Test
    void releaseResumeLockDoesNotFallbackToPlainDeleteWhenTokenedLuaFails() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
        ContextPersistenceService service = service(redis, executionMapper);
        doThrow(new IllegalStateException("redis fail"))
                .when(redis).execute(any(), eq(java.util.List.of("canvas:resume-lock:10:user-1")), eq("token-1"));

        service.releaseResumeLock(10L, "user-1", "token-1");

        verify(redis, never()).delete("canvas:resume-lock:10:user-1");
    }

    private static ContextPersistenceService service(StringRedisTemplate redis,
                                                     CanvasExecutionMapper executionMapper) {
        return new ContextPersistenceService(
                redis, OBJECT_MAPPER, new RedisKeyUtil(), executionMapper);
    }
}
