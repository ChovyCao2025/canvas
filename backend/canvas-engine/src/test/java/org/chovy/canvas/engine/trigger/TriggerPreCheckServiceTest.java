package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.execution.CanvasUserQuotaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TriggerPreCheckServiceTest {

    // Constant mirrored from TriggerPreCheckService — must stay in sync
    private static final String QUOTA_KEY = "canvas:quota:";

    @Mock
    CanvasMapper canvasMapper;

    @Mock
    CanvasUserQuotaMapper quotaMapper;

    @Mock
    StringRedisTemplate redis;

    @Mock
    ValueOperations<String, String> valueOps;

    @InjectMocks
    TriggerPreCheckService service;

    private static final Long   CANVAS_ID = 42L;
    private static final String USER_ID   = "user-1";

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
    }

    /** Builds a minimal published Canvas with only perUserTotalLimit set. */
    private Canvas canvasWithTotalLimit(int limit) {
        Canvas c = new Canvas();
        c.setId(CANVAS_ID);
        c.setStatus(1);                      // PUBLISHED
        c.setPerUserTotalLimit(limit);
        // All other limits null — only check #5 is exercised
        return c;
    }

    // ─── perUserTotalLimit ───────────────────────────────────────────────────

    @Test
    void perUserTotalLimit_rejectsWhenAtLimit() {
        int limit = 3;
        Canvas canvas = canvasWithTotalLimit(limit);

        String key = QUOTA_KEY + "total:" + CANVAS_ID + ":" + USER_ID;

        // INCR returns limit+1 — over quota
        when(valueOps.increment(key)).thenReturn((long) limit + 1);

        assertThatThrownBy(() -> service.check(canvas, USER_ID))
                .isInstanceOf(TriggerPreCheckService.TriggerRejectedException.class)
                .hasMessageContaining("用户总触发次数已达上限")
                .extracting(e -> ((TriggerPreCheckService.TriggerRejectedException) e).getCode())
                .isEqualTo("QUOTA_002");

        // Rollback decrement must be called exactly once
        verify(valueOps, times(1)).decrement(key);
    }

    @Test
    void perUserTotalLimit_allowsWhenUnderLimit() {
        int limit = 3;
        Canvas canvas = canvasWithTotalLimit(limit);

        String key = QUOTA_KEY + "total:" + CANVAS_ID + ":" + USER_ID;

        // INCR returns 2 — still under limit
        when(valueOps.increment(key)).thenReturn(2L);

        assertThatCode(() -> service.check(canvas, USER_ID))
                .doesNotThrowAnyException();

        // No rollback decrement should occur
        verify(valueOps, never()).decrement(key);
    }

    @Test
    void cooldownRejectsWithRedisSetNxBeforeQuotaCounters() {
        Canvas canvas = new Canvas();
        canvas.setId(CANVAS_ID);
        canvas.setStatus(1);
        canvas.setCooldownSeconds(60);
        canvas.setPerUserDailyLimit(10);

        String cooldownKey = QUOTA_KEY + "cooldown:" + CANVAS_ID + ":" + USER_ID;
        when(valueOps.setIfAbsent(cooldownKey, "1", Duration.ofSeconds(60))).thenReturn(false);

        assertThatThrownBy(() -> service.check(canvas, USER_ID))
                .isInstanceOf(TriggerPreCheckService.TriggerRejectedException.class)
                .hasMessageContaining("冷却期内")
                .extracting(e -> ((TriggerPreCheckService.TriggerRejectedException) e).getCode())
                .isEqualTo("QUOTA_003");

        verify(valueOps, never()).increment(anyString());
    }
}
