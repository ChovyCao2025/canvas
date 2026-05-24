package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.dataobject.CanvasUserQuotaDO;
import org.chovy.canvas.dal.mapper.CanvasUserQuotaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TriggerPreCheckServiceTest {

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

    private CanvasDO canvasWithTotalLimit(int limit) {
        CanvasDO c = new CanvasDO();
        c.setId(CANVAS_ID);
        c.setStatus(1);
        c.setPerUserTotalLimit(limit);
        return c;
    }

    private CanvasDO canvasWithAllQuotaLimits() {
        CanvasDO c = new CanvasDO();
        c.setId(CANVAS_ID);
        c.setStatus(1);
        c.setMaxTotalExecutions(10);
        c.setPerUserDailyLimit(3);
        c.setPerUserTotalLimit(5);
        return c;
    }

    @Test
    void checkWithoutQuotaAccounting_doesNotConsumeRedisOrPersistQuotaCounters() {
        CanvasDO canvas = canvasWithAllQuotaLimits();

        assertThatCode(() -> service.checkWithoutQuotaAccounting(canvas, USER_ID))
                .doesNotThrowAnyException();

        verify(redis, never()).opsForValue();
        verifyNoInteractions(valueOps);
        verify(quotaMapper, never()).insert(any(CanvasUserQuotaDO.class));
        verify(quotaMapper, never()).update(any(CanvasUserQuotaDO.class), any());
    }

    @Test
    void checkWithoutQuotaAccounting_rejectsUnpublishedCanvas() {
        CanvasDO canvas = canvasWithAllQuotaLimits();
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
        CanvasDO canvas = canvasWithAllQuotaLimits();
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
    void checkWithoutQuotaAccounting_rejectsExistingCooldownWithoutConsumingQuota() {
        CanvasDO canvas = canvasWithAllQuotaLimits();
        canvas.setCooldownSeconds(60);
        CanvasUserQuotaDO quota = new CanvasUserQuotaDO();
        quota.setLastTriggerAt(LocalDateTime.now().minusSeconds(5));
        when(quotaMapper.selectOne(any())).thenReturn(quota);

        assertThatThrownBy(() -> service.checkWithoutQuotaAccounting(canvas, USER_ID))
                .isInstanceOf(TriggerPreCheckService.TriggerRejectedException.class)
                .hasMessageContaining("冷却期内")
                .extracting(e -> ((TriggerPreCheckService.TriggerRejectedException) e).getCode())
                .isEqualTo("QUOTA_003");

        verify(redis, never()).opsForValue();
        verifyNoInteractions(valueOps);
        verify(quotaMapper, never()).insert(any(CanvasUserQuotaDO.class));
        verify(quotaMapper, never()).update(any(CanvasUserQuotaDO.class), any());
    }

    @Test
    void perUserTotalLimit_rejectsWhenAtLimit() {
        int limit = 3;
        CanvasDO canvas = canvasWithTotalLimit(limit);

        String key = QUOTA_KEY + "total:" + CANVAS_ID + ":" + USER_ID;
        when(valueOps.increment(key)).thenReturn((long) limit + 1);

        assertThatThrownBy(() -> service.check(canvas, USER_ID))
                .isInstanceOf(TriggerPreCheckService.TriggerRejectedException.class)
                .hasMessageContaining("用户总触发次数已达上限")
                .extracting(e -> ((TriggerPreCheckService.TriggerRejectedException) e).getCode())
                .isEqualTo("QUOTA_002");

        verify(valueOps, times(1)).decrement(key);
    }

    @Test
    void perUserTotalLimit_allowsWhenUnderLimit() {
        int limit = 3;
        CanvasDO canvas = canvasWithTotalLimit(limit);

        String key = QUOTA_KEY + "total:" + CANVAS_ID + ":" + USER_ID;
        when(valueOps.increment(key)).thenReturn(2L);

        assertThatCode(() -> service.check(canvas, USER_ID))
                .doesNotThrowAnyException();

        verify(valueOps, never()).decrement(key);
    }

    @Test
    void cooldownRejectsWithRedisSetNxBeforeQuotaCounters() {
        CanvasDO canvas = new CanvasDO();
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
