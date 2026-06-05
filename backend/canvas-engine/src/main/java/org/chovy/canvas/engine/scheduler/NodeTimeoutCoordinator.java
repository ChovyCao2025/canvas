package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.infrastructure.reactor.TrackedReactiveTaskRegistry;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Coordinates one-shot timeout scheduling for waiting DAG nodes.
 */
@Component
public class NodeTimeoutCoordinator {

    boolean scheduleOnce(ExecutionContext ctx,
                         String timerKey,
                         String startTimeKey,
                         String taskName,
                         int timeoutSec,
                         Scheduler scheduler,
                         TrackedReactiveTaskRegistry reactiveTaskRegistry,
                         Runnable timeoutAction,
                         Consumer<Throwable> onError) {
        if (!ctx.getScheduledHubTimeouts().add(timerKey)) {
            return false;
        }
        ctx.getHubStartTimes().putIfAbsent(startTimeKey, System.currentTimeMillis());
        reactiveTaskRegistry.submit(
                taskName,
                Mono.delay(Duration.ofSeconds(timeoutSec), scheduler)
                        .doOnNext(__ -> timeoutAction.run())
                        .then(),
                onError);
        return true;
    }

    boolean hasElapsed(ExecutionContext ctx, String startTimeKey, int timeoutSec) {
        return hasElapsed(ctx, startTimeKey, timeoutSec, System.currentTimeMillis());
    }

    boolean hasElapsed(ExecutionContext ctx, String startTimeKey, int timeoutSec, long nowMs) {
        long start = ctx.getHubStartTimes().getOrDefault(startTimeKey, nowMs);
        return nowMs - start > (long) timeoutSec * 1000;
    }
}
