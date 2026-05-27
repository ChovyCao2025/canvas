package org.chovy.canvas.engine.wait;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.CanvasWaitSubscriptionDO;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WaitResumeServiceTest {

    @Test
    void resumeEventWaitsSkipsSubscriptionsWhoseEventFiltersDoNotMatch() {
        WaitSubscriptionService waitSubscriptionService = mock(WaitSubscriptionService.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        WaitResumeService service = new WaitResumeService(
                waitSubscriptionService, executionService, new ObjectMapper());
        CanvasWaitSubscriptionDO wait = waitRecord();
        wait.setEventFilters("{\"amount\":{\"gt\":100}}");
        when(waitSubscriptionService.findActiveEventWaits("ORDER_PAID", "user-1")).thenReturn(List.of(wait));

        int resumed = service.resumeEventWaits("ORDER_PAID", "user-1", Map.of("amount", 50), "evt-1");

        assertThat(resumed).isZero();
        verify(waitSubscriptionService, never()).completeWait(any(), any());
        verify(executionService, never()).trigger(any(), any(), any(), any(), any(), any(), any(), eq(false));
    }

    @Test
    void resumeEventWaitsCompletesAndTriggersWhenEventFiltersMatch() {
        WaitSubscriptionService waitSubscriptionService = mock(WaitSubscriptionService.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        WaitResumeService service = new WaitResumeService(
                waitSubscriptionService, executionService, new ObjectMapper());
        CanvasWaitSubscriptionDO wait = waitRecord();
        wait.setEventFilters("{\"amount\":{\"gt\":100}}");
        when(waitSubscriptionService.findActiveEventWaits("ORDER_PAID", "user-1")).thenReturn(List.of(wait));
        when(waitSubscriptionService.completeWait(eq(1L), any())).thenReturn(1);
        when(executionService.trigger(any(), any(), any(), any(), any(), any(), any(), eq(false)))
                .thenReturn(Mono.just(Map.of()));

        int resumed = service.resumeEventWaits("ORDER_PAID", "user-1", Map.of("amount", 150), "evt-2");

        assertThat(resumed).isEqualTo(1);
        verify(executionService).trigger(eq(10L), eq("user-1"), eq(TriggerType.WAIT_RESUME),
                eq(NodeType.WAIT), eq("wait-1"), any(), any(), eq(false));
    }

    private CanvasWaitSubscriptionDO waitRecord() {
        CanvasWaitSubscriptionDO wait = new CanvasWaitSubscriptionDO();
        wait.setId(1L);
        wait.setExecutionId("exec-1");
        wait.setCanvasId(10L);
        wait.setUserId("user-1");
        wait.setNodeId("wait-1");
        wait.setWaitType(WaitSubscriptionService.WAIT_TYPE_EVENT);
        wait.setEventCode("ORDER_PAID");
        wait.setResumePayload("{}");
        return wait;
    }
}
