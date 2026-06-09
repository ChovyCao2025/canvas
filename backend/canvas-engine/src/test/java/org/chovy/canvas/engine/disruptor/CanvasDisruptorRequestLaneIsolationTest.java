package org.chovy.canvas.engine.disruptor;

import org.chovy.canvas.engine.lane.ExecutionLane;
import org.chovy.canvas.engine.lane.ExecutionLaneWorkerRegistry;
import org.chovy.canvas.engine.lifecycle.ExecutionLifecycleGate;
import org.chovy.canvas.engine.reactive.BackgroundSubscriptionRegistry;
import org.chovy.canvas.engine.request.CanvasExecutionRequestExecutor;
import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasDisruptorRequestLaneIsolationTest {

    private CanvasDisruptorService service;

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.shutdown();
        }
    }

    @Test
    void requestWorkerHoldsLanePermitUntilPersistentExecutionCompletes() throws Exception {
        CanvasExecutionRequestExecutor requestExecutor = mock(CanvasExecutionRequestExecutor.class);
        BackgroundSubscriptionRegistry backgroundSubscriptions = new BackgroundSubscriptionRegistry();
        ExecutionLaneWorkerRegistry laneRegistry = new ExecutionLaneWorkerRegistry(
                true, Map.of(ExecutionLane.HEAVY, 1));
        CountDownLatch subscribed = new CountDownLatch(1);
        when(requestExecutor.execute(eq("req-1"), eq(ExecutionLane.HEAVY)))
                .thenReturn(Mono.<Void>never().doOnSubscribe(subscription -> subscribed.countDown()));
        service = new CanvasDisruptorService(
                mock(CanvasExecutionService.class),
                requestExecutor,
                mock(CanvasMetrics.class),
                1024,
                1,
                backgroundSubscriptions,
                new ExecutionLifecycleGate(),
                laneRegistry);

        service.publishRequest("req-1", ExecutionLane.HEAVY);

        assertThat(subscribed.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(laneRegistry.availablePermits(ExecutionLane.HEAVY)).isZero();
        verify(requestExecutor).execute("req-1", ExecutionLane.HEAVY);
    }
}
