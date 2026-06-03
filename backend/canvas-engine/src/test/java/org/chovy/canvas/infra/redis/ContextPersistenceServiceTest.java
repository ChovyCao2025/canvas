package org.chovy.canvas.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
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

class ContextPersistenceServiceTest {

    @Test
    void saveStoresRawExecutionContextWithoutMaskingRuntimeValues() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        ContextPersistenceService service = new ContextPersistenceService(
                redis, new ObjectMapper(), new RedisKeyUtil());
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
}
