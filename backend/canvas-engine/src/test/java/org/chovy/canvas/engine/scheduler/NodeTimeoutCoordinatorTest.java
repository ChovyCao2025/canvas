package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.infrastructure.reactor.TrackedReactiveTaskRegistry;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class NodeTimeoutCoordinatorTest {

    private final NodeTimeoutCoordinator coordinator = new NodeTimeoutCoordinator();

    @Test
    void scheduleOnceRecordsStartTimeAndSubmitsDelayTask() {
        ExecutionContext ctx = new ExecutionContext();
        TrackedReactiveTaskRegistry registry = mock(TrackedReactiveTaskRegistry.class);

        boolean scheduled = coordinator.scheduleOnce(
                ctx,
                "timer-1",
                "start-1",
                "task-1",
                30,
                Schedulers.parallel(),
                registry,
                () -> {
                },
                ignored -> {
                });

        assertThat(scheduled).isTrue();
        assertThat(ctx.getScheduledHubTimeouts()).contains("timer-1");
        assertThat(ctx.getHubStartTimes()).containsKey("start-1");
        verify(registry).submit(eq("task-1"), any(Mono.class), any());
    }

    @Test
    void scheduleOnceRejectsDuplicateTimerKeys() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.getScheduledHubTimeouts().add("timer-1");
        TrackedReactiveTaskRegistry registry = mock(TrackedReactiveTaskRegistry.class);

        boolean scheduled = coordinator.scheduleOnce(
                ctx,
                "timer-1",
                "start-1",
                "task-1",
                30,
                Schedulers.parallel(),
                registry,
                () -> {
                },
                ignored -> {
                });

        assertThat(scheduled).isFalse();
        assertThat(ctx.getHubStartTimes()).doesNotContainKey("start-1");
        verify(registry, never()).submit(any(), any(Mono.class), any());
    }

    @Test
    void hasElapsedUsesRecordedStartTime() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.getHubStartTimes().put("timer-1", 1_000L);

        assertThat(coordinator.hasElapsed(ctx, "timer-1", 10, 11_001L)).isTrue();
        assertThat(coordinator.hasElapsed(ctx, "timer-1", 10, 10_999L)).isFalse();
    }
}
