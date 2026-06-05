package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.engine.handler.HandlerRegistry;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DagEngineLifecycleTest {

    @Test
    void shutdownDisposesOwnedSpecialNodeTimeoutScheduler() {
        Scheduler scheduler = Schedulers.newSingle("dag-engine-lifecycle-test");
        DagEngine engine = engine(scheduler, true);

        engine.shutdownSpecialNodeTimeoutScheduler();

        assertThat(scheduler.isDisposed()).isTrue();
    }

    private DagEngine engine(Scheduler scheduler,
                             boolean ownsScheduler) {
        return new DagEngine(
                mock(HandlerRegistry.class),
                mock(TraceWriteBuffer.class),
                mock(CircuitBreakerRegistry.class),
                mock(CanvasMetrics.class),
                mock(com.fasterxml.jackson.databind.ObjectMapper.class),
                mock(ContextPersistenceService.class),
                mock(org.chovy.canvas.engine.trigger.CanvasExecutionService.class),
                scheduler,
                ownsScheduler,
                mock(ExecutionDlqWriter.class),
                mock(NodeResultRouter.class)
        );
    }
}
