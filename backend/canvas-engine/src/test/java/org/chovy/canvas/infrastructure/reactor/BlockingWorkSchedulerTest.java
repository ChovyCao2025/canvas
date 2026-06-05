package org.chovy.canvas.infrastructure.reactor;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class BlockingWorkSchedulerTest {

    private final BlockingWorkScheduler scheduler = new BlockingWorkScheduler();

    @Test
    void callRunsBlockingWorkOnBoundedElastic() {
        StepVerifier.create(scheduler.call("test-call", () -> Thread.currentThread().getName()))
                .assertNext(threadName -> assertThat(threadName).contains("boundedElastic"))
                .verifyComplete();
    }

    @Test
    void getMovesCallableToBoundedElasticForSynchronousCallers() {
        String threadName = scheduler.get("test-get", () -> Thread.currentThread().getName());

        assertThat(threadName).contains("boundedElastic");
    }

    @Test
    void awaitRejectsEventLoopThreads() throws Exception {
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        Thread eventLoop = new Thread(
                () -> {
                    try {
                        scheduler.await("event-loop-block", Mono.just("ok"));
                    } catch (Throwable e) {
                        thrown.set(e);
                    }
                },
                "reactor-http-nio-1");

        eventLoop.start();
        eventLoop.join();

        assertThat(thrown.get())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("event-loop-block")
                .hasMessageContaining("reactor-http-nio-1");
    }
}
