package org.chovy.canvas.engine.scheduler;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CircuitBreakerRegistryConcurrencyTest {

    @Test
    void circuitBreakerStateIsStoredInSingleAtomicSnapshot() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/chovy/canvas/engine/scheduler/CircuitBreakerRegistry.java"));

        assertThat(source).contains("AtomicReference<BreakerState>");
        assertThat(source).contains("record BreakerState");
        assertThat(source).doesNotContain("volatile State");
        assertThat(source).doesNotContain("AtomicInteger failures");
        assertThat(source).doesNotContain("AtomicInteger halfTries");
    }

    @Test
    void halfOpenProbeAllowanceIsAtomicUnderConcurrentChecks() throws Exception {
        for (int trial = 0; trial < 50; trial++) {
            CircuitBreakerRegistry.CircuitBreaker breaker =
                    new CircuitBreakerRegistry.CircuitBreaker("race", 1, 0, 1);
            breaker.recordFailure();

            int allowed = concurrentAllowedChecks(breaker, 32);

            assertThat(allowed)
                    .as("trial %s allowed more half-open probes than configured", trial)
                    .isLessThanOrEqualTo(1);
        }
    }

    private int concurrentAllowedChecks(
            CircuitBreakerRegistry.CircuitBreaker breaker,
            int concurrency
    ) throws InterruptedException {
        var executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        List<Throwable> unexpected = new ArrayList<>();

        try {
            for (int i = 0; i < concurrency; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        breaker.checkState();
                        allowed.incrementAndGet();
                    } catch (CircuitBreakerRegistry.CircuitBreakerOpenException ignored) {
                        // expected after half-open probes are exhausted
                    } catch (Throwable throwable) {
                        synchronized (unexpected) {
                            unexpected.add(throwable);
                        }
                    }
                });
            }
            assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            assertThat(unexpected).isEmpty();
            return allowed.get();
        } finally {
            executor.shutdownNow();
        }
    }
}
