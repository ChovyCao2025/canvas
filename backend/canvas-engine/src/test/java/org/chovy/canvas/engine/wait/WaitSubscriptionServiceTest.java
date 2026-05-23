package org.chovy.canvas.engine.wait;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.chovy.canvas.domain.execution.CanvasWaitSubscription;
import org.chovy.canvas.domain.execution.CanvasWaitSubscriptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitSubscriptionServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-23T10:15:30Z"),
            ZoneId.of("Asia/Shanghai")
    );

    @Mock
    CanvasWaitSubscriptionMapper mapper;

    @Test
    void createEventWait_persistsActiveSubscription() {
        WaitSubscriptionService service = new WaitSubscriptionService(mapper, CLOCK);
        LocalDateTime expiresAt = LocalDateTime.of(2026, 5, 24, 18, 0);

        CanvasWaitSubscription created = service.createEventWait(
                "exec-1",
                10L,
                20L,
                "user-1",
                "wait-1",
                "ORDER_PAID",
                "{\"amount\":{\"gt\":100}}",
                "{\"route\":\"success\"}",
                expiresAt
        );

        ArgumentCaptor<CanvasWaitSubscription> captor = ArgumentCaptor.forClass(CanvasWaitSubscription.class);
        verify(mapper).insert(captor.capture());
        CanvasWaitSubscription inserted = captor.getValue();

        assertThat(created).isSameAs(inserted);
        assertThat(inserted.getExecutionId()).isEqualTo("exec-1");
        assertThat(inserted.getCanvasId()).isEqualTo(10L);
        assertThat(inserted.getVersionId()).isEqualTo(20L);
        assertThat(inserted.getUserId()).isEqualTo("user-1");
        assertThat(inserted.getNodeId()).isEqualTo("wait-1");
        assertThat(inserted.getWaitType()).isEqualTo(WaitSubscriptionService.WAIT_TYPE_EVENT);
        assertThat(inserted.getEventCode()).isEqualTo("ORDER_PAID");
        assertThat(inserted.getEventFilters()).isEqualTo("{\"amount\":{\"gt\":100}}");
        assertThat(inserted.getResumePayload()).isEqualTo("{\"route\":\"success\"}");
        assertThat(inserted.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(inserted.getStatus()).isEqualTo(WaitSubscriptionService.STATUS_ACTIVE);
        assertThat(inserted.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 23, 18, 15, 30));
        assertThat(inserted.getUpdatedAt()).isEqualTo(inserted.getCreatedAt());
    }

    @Test
    void createGoalWait_persistsActiveGoalSubscription() {
        WaitSubscriptionService service = new WaitSubscriptionService(mapper, CLOCK);
        LocalDateTime expiresAt = LocalDateTime.of(2026, 5, 24, 18, 0);

        CanvasWaitSubscription created = service.createGoalWait(
                "exec-1",
                10L,
                20L,
                "user-1",
                "goal-1",
                "ORDER_PAID",
                "{\"sourceNodeId\":\"goal-1\"}",
                expiresAt
        );

        ArgumentCaptor<CanvasWaitSubscription> captor = ArgumentCaptor.forClass(CanvasWaitSubscription.class);
        verify(mapper).insert(captor.capture());
        CanvasWaitSubscription inserted = captor.getValue();

        assertThat(created).isSameAs(inserted);
        assertThat(inserted.getExecutionId()).isEqualTo("exec-1");
        assertThat(inserted.getCanvasId()).isEqualTo(10L);
        assertThat(inserted.getVersionId()).isEqualTo(20L);
        assertThat(inserted.getUserId()).isEqualTo("user-1");
        assertThat(inserted.getNodeId()).isEqualTo("goal-1");
        assertThat(inserted.getWaitType()).isEqualTo(WaitSubscriptionService.WAIT_TYPE_GOAL);
        assertThat(inserted.getEventCode()).isEqualTo("ORDER_PAID");
        assertThat(inserted.getEventFilters()).isNull();
        assertThat(inserted.getResumePayload()).isEqualTo("{\"sourceNodeId\":\"goal-1\"}");
        assertThat(inserted.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(inserted.getStatus()).isEqualTo(WaitSubscriptionService.STATUS_ACTIVE);
        assertThat(inserted.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 23, 18, 15, 30));
        assertThat(inserted.getUpdatedAt()).isEqualTo(inserted.getCreatedAt());
    }

    @Test
    void findActiveEventWaits_returnsMapperResults() {
        WaitSubscriptionService service = new WaitSubscriptionService(mapper, CLOCK);
        CanvasWaitSubscription wait = new CanvasWaitSubscription();
        wait.setId(1L);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(wait));

        assertThat(service.findActiveEventWaits("ORDER_PAID", "user-1")).containsExactly(wait);

        verify(mapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void completeWait_marksSubscriptionCompletedWithResumePayload() {
        WaitSubscriptionService service = new WaitSubscriptionService(mapper, CLOCK);

        service.completeWait(99L, "{\"eventId\":\"evt-1\"}");

        ArgumentCaptor<CanvasWaitSubscription> entityCaptor = ArgumentCaptor.forClass(CanvasWaitSubscription.class);
        verify(mapper).update(entityCaptor.capture(), any(LambdaUpdateWrapper.class));

        CanvasWaitSubscription update = entityCaptor.getValue();
        assertThat(update.getStatus()).isEqualTo(WaitSubscriptionService.STATUS_COMPLETED);
        assertThat(update.getResumePayload()).isEqualTo("{\"eventId\":\"evt-1\"}");
        assertThat(update.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 23, 18, 15, 30));
    }

    @Test
    void expireWait_marksSubscriptionExpiredWithReasonPayload() {
        WaitSubscriptionService service = new WaitSubscriptionService(mapper, CLOCK);

        service.expireWait(99L, "{\"reason\":\"timeout\"}");

        ArgumentCaptor<CanvasWaitSubscription> entityCaptor = ArgumentCaptor.forClass(CanvasWaitSubscription.class);
        verify(mapper).update(entityCaptor.capture(), any(LambdaUpdateWrapper.class));

        CanvasWaitSubscription update = entityCaptor.getValue();
        assertThat(update.getStatus()).isEqualTo(WaitSubscriptionService.STATUS_EXPIRED);
        assertThat(update.getResumePayload()).isEqualTo("{\"reason\":\"timeout\"}");
        assertThat(update.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 23, 18, 15, 30));
    }
}
