package org.chovy.canvas.engine.scheduler;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CircuitBreakerRegistryTest {

    @Test
    void transitionStateIsStoredAsSingleAtomicSnapshot() throws Exception {
        Class<CircuitBreakerRegistry.CircuitBreaker> type = CircuitBreakerRegistry.CircuitBreaker.class;

        Field stateRef = type.getDeclaredField("stateRef");

        assertThat(stateRef.getType()).isEqualTo(AtomicReference.class);
    }

    @Test
    void constructorRejectsInvalidThresholds() {
        assertThatThrownBy(() -> new CircuitBreakerRegistry.CircuitBreaker("api", 0, 30, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("failureThreshold");
        assertThatThrownBy(() -> new CircuitBreakerRegistry.CircuitBreaker("api", 5, -1, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("openDurationSec");
        assertThatThrownBy(() -> new CircuitBreakerRegistry.CircuitBreaker("api", 5, 30, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("halfOpenAttempts");
    }

    @Test
    void opensAfterThresholdAndRejectsBeforeCooldownElapses() {
        CircuitBreakerRegistry.CircuitBreaker breaker =
                new CircuitBreakerRegistry.CircuitBreaker("api", 2, 30, 1);

        breaker.recordFailure();
        breaker.checkState();
        breaker.recordFailure();

        assertThatThrownBy(breaker::checkState)
                .isInstanceOf(CircuitBreakerRegistry.CircuitBreakerOpenException.class)
                .hasMessageContaining("api");
    }

    @Test
    void concurrentFailuresOpenOnceAtThreshold() throws Exception {
        CircuitBreakerRegistry.CircuitBreaker breaker =
                new CircuitBreakerRegistry.CircuitBreaker("api", 50, 30, 1);
        int threads = 200;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        var executor = Executors.newFixedThreadPool(16);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    breaker.recordFailure();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();

        assertThat(breaker.currentState()).isEqualTo(CircuitBreakerRegistry.CircuitBreaker.State.OPEN);
        assertThat(breaker.failureCount()).isEqualTo(50);
    }

    @Test
    void halfOpenAllowsOnlyConfiguredConcurrentProbeAttempts() throws Exception {
        CircuitBreakerRegistry.CircuitBreaker breaker =
                new CircuitBreakerRegistry.CircuitBreaker("api", 1, 1, 3);
        breaker.recordFailure();
        Thread.sleep(1_100);

        int threads = 12;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger allowed = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    breaker.checkState();
                    allowed.incrementAndGet();
                } catch (CircuitBreakerRegistry.CircuitBreakerOpenException e) {
                    rejected.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();

        assertThat(allowed.get()).isEqualTo(3);
        assertThat(rejected.get()).isEqualTo(9);
        assertThat(breaker.currentState()).isEqualTo(CircuitBreakerRegistry.CircuitBreaker.State.HALF_OPEN);
        assertThat(breaker.halfOpenTryCount()).isEqualTo(3);
    }
}
