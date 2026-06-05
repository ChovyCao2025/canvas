package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.lane.ExecutionLane;
import org.chovy.canvas.engine.lane.ExecutionLaneAdmissionResult;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.junit.jupiter.api.Test;
import reactor.core.Disposables;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecutionLaneDispatcherTest {

    @Test
    void dryRunSkipsSlotAcquisition() {
        InFlightExecutionRegistry registry = mock(InFlightExecutionRegistry.class);
        ExecutionLaneDispatcher dispatcher = new ExecutionLaneDispatcher(
                registry,
                mock(ContextPersistenceService.class));

        ExecutionLaneDispatcher.SlotAcquisitionResult result = dispatcher.tryAcquireSlot(
                10L, context(), ExecutionLane.LIGHT, 100, 10, 200,
                true, false, null);

        assertThat(result.isOverflow()).isFalse();
        assertThat(result.slot()).isNull();
        verify(registry, never()).tryAcquire(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void allowedRegistryResultReturnsSlot() {
        InFlightExecutionRegistry registry = mock(InFlightExecutionRegistry.class);
        reactor.core.Disposable.Swap slot = Disposables.swap();
        when(registry.tryAcquire(10L, "exec-1", ExecutionLane.HEAVY, 100, 20, 300))
                .thenReturn(ExecutionLaneAdmissionResult.allowed(slot, 1, 1, 1));
        ExecutionLaneDispatcher dispatcher = new ExecutionLaneDispatcher(
                registry,
                mock(ContextPersistenceService.class));

        ExecutionLaneDispatcher.SlotAcquisitionResult result = dispatcher.tryAcquireSlot(
                10L, context(), ExecutionLane.HEAVY, 100, 20, 300,
                false, false, null);

        assertThat(result.isOverflow()).isFalse();
        assertThat(result.slot()).isSameAs(slot);
    }

    @Test
    void rejectedRegistryResultReleasesLocksAndReturnsOverflowResponse() {
        InFlightExecutionRegistry registry = mock(InFlightExecutionRegistry.class);
        when(registry.tryAcquire(10L, "exec-1", ExecutionLane.RETRY, 100, 20, 300))
                .thenReturn(ExecutionLaneAdmissionResult.rejected(
                        ExecutionLaneAdmissionResult.Reason.LANE_LIMIT, 7, 20, 88));
        ContextPersistenceService ctxStore = mock(ContextPersistenceService.class);
        ExecutionContext ctx = context();
        ctx.setResumeLockToken("resume-token");
        ExecutionLaneDispatcher dispatcher = new ExecutionLaneDispatcher(registry, ctxStore);

        ExecutionLaneDispatcher.SlotAcquisitionResult result = dispatcher.tryAcquireSlot(
                10L, ctx, ExecutionLane.RETRY, 100, 20, 300,
                false, true, "dedup-key");

        assertThat(result.isOverflow()).isTrue();
        Map<String, Object> response = result.overflowMono().block();
        assertThat(response).containsEntry(MapFieldKeys.OVERFLOW, "concurrency_limit_reached");
        assertThat(response).containsEntry(MapFieldKeys.EXECUTION_LANE, ExecutionLane.RETRY.name());
        assertThat(response).containsEntry(MapFieldKeys.ADMISSION_REASON,
                ExecutionLaneAdmissionResult.Reason.LANE_LIMIT.name());
        assertThat(response).containsEntry(MapFieldKeys.ACTIVE, 7);
        assertThat(response).containsEntry(MapFieldKeys.LANE_ACTIVE, 20);
        assertThat(response).containsEntry(MapFieldKeys.GLOBAL_ACTIVE, 88);
        assertThat(response).containsEntry(MapFieldKeys.LIMIT, 100);
        verify(ctxStore).releaseResumeLock(10L, "user-1", "resume-token");
        verify(ctxStore).releaseDedup("dedup-key");
    }

    private ExecutionContext context() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-1");
        ctx.setCanvasId(10L);
        ctx.setUserId("user-1");
        return ctx;
    }
}
