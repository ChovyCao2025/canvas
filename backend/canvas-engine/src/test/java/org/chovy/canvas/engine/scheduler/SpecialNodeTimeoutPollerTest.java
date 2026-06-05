package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.infrastructure.redis.RedisDelayQueue;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpecialNodeTimeoutPollerTest {

    @Test
    void pollDueTimeoutsTriggersInternalContinuation() {
        RedisDelayQueue delayQueue = mock(RedisDelayQueue.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        RedisDelayQueue.SpecialNodeTimeout item = new RedisDelayQueue.SpecialNodeTimeout(
                "exec-timeout-test:hub:1000",
                "exec-timeout-test",
                10L,
                1L,
                "user-1",
                "hub",
                NodeType.HUB,
                TriggerType.HUB_TIMEOUT,
                "hub",
                1_000L,
                8_000L,
                7L,
                "WAITING");
        Map<String, Object> payload = Map.of(
                MapFieldKeys.EXECUTION_ID, "exec-timeout-test",
                MapFieldKeys.VERSION_ID, 1L,
                MapFieldKeys.TIMEOUT_TIMER_KEY, "hub",
                MapFieldKeys.TIMEOUT_SCHEDULED_AT_EPOCH_MS, 1_000L,
                MapFieldKeys.TIMEOUT_FIRE_AT_EPOCH_MS, 8_000L,
                MapFieldKeys.TIMEOUT_SECONDS, 7L);
        when(delayQueue.pollDueSpecialNodeTimeouts()).thenReturn(List.of(item));
        when(executionService.trigger(10L, "user-1", TriggerType.HUB_TIMEOUT,
                NodeType.HUB, "hub", payload,
                "exec-timeout-test:hub:1000", false))
                .thenReturn(Mono.just(Map.of()));
        SpecialNodeTimeoutPoller poller = new SpecialNodeTimeoutPoller(delayQueue, executionService);

        poller.pollDueTimeouts();

        verify(executionService).trigger(10L, "user-1", TriggerType.HUB_TIMEOUT,
                NodeType.HUB, "hub", payload,
                "exec-timeout-test:hub:1000", false);
        verify(delayQueue).ackSpecialNodeTimeout(item);
    }

    @Test
    void triggerFailureRequeuesClaimedTimeout() {
        RedisDelayQueue delayQueue = mock(RedisDelayQueue.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        RedisDelayQueue.SpecialNodeTimeout item = new RedisDelayQueue.SpecialNodeTimeout(
                "exec-timeout-test:hub:1000",
                "exec-timeout-test",
                10L,
                1L,
                "user-1",
                "hub",
                NodeType.HUB,
                TriggerType.HUB_TIMEOUT,
                "hub",
                1_000L,
                8_000L,
                7L,
                "WAITING");
        when(delayQueue.pollDueSpecialNodeTimeouts()).thenReturn(List.of(item));
        when(executionService.trigger(10L, "user-1", TriggerType.HUB_TIMEOUT,
                NodeType.HUB, "hub", SpecialNodeTimeoutPoller.timeoutPayload(item),
                "exec-timeout-test:hub:1000", false))
                .thenReturn(Mono.error(new RuntimeException("temporary")));
        SpecialNodeTimeoutPoller poller = new SpecialNodeTimeoutPoller(delayQueue, executionService);

        poller.pollDueTimeouts();

        verify(delayQueue).requeueSpecialNodeTimeout(item);
    }

    @Test
    void deferredTriggerResultRequeuesClaimedTimeout() {
        RedisDelayQueue delayQueue = mock(RedisDelayQueue.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        RedisDelayQueue.SpecialNodeTimeout item = new RedisDelayQueue.SpecialNodeTimeout(
                "exec-timeout-test:hub:1000",
                "exec-timeout-test",
                10L,
                1L,
                "user-1",
                "hub",
                NodeType.HUB,
                TriggerType.HUB_TIMEOUT,
                "hub",
                1_000L,
                8_000L,
                7L,
                "WAITING");
        when(delayQueue.pollDueSpecialNodeTimeouts()).thenReturn(List.of(item));
        when(executionService.trigger(10L, "user-1", TriggerType.HUB_TIMEOUT,
                NodeType.HUB, "hub", SpecialNodeTimeoutPoller.timeoutPayload(item),
                "exec-timeout-test:hub:1000", false))
                .thenReturn(Mono.just(Map.of(MapFieldKeys.SKIPPED, "resume-lock")));
        SpecialNodeTimeoutPoller poller = new SpecialNodeTimeoutPoller(delayQueue, executionService);

        poller.pollDueTimeouts();

        verify(delayQueue).requeueSpecialNodeTimeout(item);
        verify(delayQueue, never()).ackSpecialNodeTimeout(item);
    }
}
