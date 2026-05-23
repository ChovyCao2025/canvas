package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.execution.CanvasUserQuota;
import org.chovy.canvas.domain.execution.CanvasUserQuotaMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
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

    private Canvas canvasWithAllQuotaLimits() {
        Canvas c = new Canvas();
        c.setId(CANVAS_ID);
        c.setStatus(1);
        c.setMaxTotalExecutions(10);
        c.setPerUserDailyLimit(3);
        c.setPerUserTotalLimit(5);
        return c;
    }

    // ─── checkWithoutQuotaAccounting ────────────────────────────────────────

    @Test
    void checkWithoutQuotaAccounting_doesNotConsumeRedisOrPersistQuotaCounters() {
        Canvas canvas = canvasWithAllQuotaLimits();

        assertThatCode(() -> service.checkWithoutQuotaAccounting(canvas, USER_ID))
                .doesNotThrowAnyException();

        verify(redis, never()).opsForValue();
        verifyNoInteractions(valueOps);
        verify(quotaMapper, never()).insert(any(CanvasUserQuota.class));
        verify(quotaMapper, never()).update(any(CanvasUserQuota.class), any());
    }

    @Test
    void checkWithoutQuotaAccounting_rejectsUnpublishedCanvas() {
        Canvas canvas = canvasWithAllQuotaLimits();
        canvas.setStatus(0);

        assertThatThrownBy(() -> service.checkWithoutQuotaAccounting(canvas, USER_ID))
                .isInstanceOf(TriggerPreCheckService.TriggerRejectedException.class)
                .hasMessageContaining("画布未发布")
                .extracting(e -> ((TriggerPreCheckService.TriggerRejectedException) e).getCode())
                .isEqualTo("QUOTA_006");

        verify(redis, never()).opsForValue();
        verifyNoInteractions(valueOps);
    }

    @Test
    void checkWithoutQuotaAccounting_rejectsFutureValidStart() {
        Canvas canvas = canvasWithAllQuotaLimits();
        canvas.setValidStart(LocalDateTime.now().plusMinutes(1));

        assertThatThrownBy(() -> service.checkWithoutQuotaAccounting(canvas, USER_ID))
                .isInstanceOf(TriggerPreCheckService.TriggerRejectedException.class)
                .hasMessageContaining("活动尚未开始")
                .extracting(e -> ((TriggerPreCheckService.TriggerRejectedException) e).getCode())
                .isEqualTo("QUOTA_005");

        verify(redis, never()).opsForValue();
        verifyNoInteractions(valueOps);
    }

    @Test
    void checkWithoutQuotaAccounting_rejectsCooldownWithoutConsumingQuota() {
        Canvas canvas = canvasWithAllQuotaLimits();
        canvas.setCooldownSeconds(60);
        CanvasUserQuota quota = new CanvasUserQuota();
        quota.setLastTriggerAt(LocalDateTime.now().minusSeconds(5));
        when(quotaMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(quota);

        assertThatThrownBy(() -> service.checkWithoutQuotaAccounting(canvas, USER_ID))
                .isInstanceOf(TriggerPreCheckService.TriggerRejectedException.class)
                .hasMessageContaining("冷却期内")
                .extracting(e -> ((TriggerPreCheckService.TriggerRejectedException) e).getCode())
                .isEqualTo("QUOTA_003");

        verify(redis, never()).opsForValue();
        verifyNoInteractions(valueOps);
        verify(quotaMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(quotaMapper, never()).insert(any(CanvasUserQuota.class));
        verify(quotaMapper, never()).update(any(CanvasUserQuota.class), any());
    }

    // ─── perUserTotalLimit ───────────────────────────────────────────────────

    @Test
    void perUserTotalLimit_rejectsWhenAtLimit() {
        int limit = 3;
        Canvas canvas = canvasWithTotalLimit(limit);

        String today = LocalDate.now().toString();
        String key   = QUOTA_KEY + "total:" + CANVAS_ID + ":" + USER_ID + ":" + today;

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

        String today = LocalDate.now().toString();
        String key   = QUOTA_KEY + "total:" + CANVAS_ID + ":" + USER_ID + ":" + today;

        // INCR returns 2 — still under limit
        when(valueOps.increment(key)).thenReturn(2L);
        // quotaMapper stubbed to return null defensively for the async updateQuotaAsync() virtual thread
        // (cooldownSeconds is null on this canvas so quotaMapper.selectOne() for check #6 is not called)
        Mockito.lenient().when(quotaMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThatCode(() -> service.check(canvas, USER_ID))
                .doesNotThrowAnyException();

        // No rollback decrement should occur
        verify(valueOps, never()).decrement(key);
    }
}
