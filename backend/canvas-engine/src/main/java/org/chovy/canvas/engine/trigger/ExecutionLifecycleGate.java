package org.chovy.canvas.engine.trigger;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Process-local shutdown gate for canvas execution entrypoints.
 */
@Slf4j
@Component("triggerExecutionLifecycleGate")
public class ExecutionLifecycleGate {

    private final Duration drainTimeout;
    private final AtomicBoolean accepting = new AtomicBoolean(true);
    private final AtomicInteger inFlight = new AtomicInteger();
    private final Object monitor = new Object();

    public ExecutionLifecycleGate(
            @Value("${canvas.execution.shutdown-drain-timeout-ms:10000}") long drainTimeoutMs) {
        this.drainTimeout = Duration.ofMillis(Math.max(0, drainTimeoutMs));
    }

    public <T> Mono<T> guard(Mono<T> mono) {
        return Mono.defer(() -> {
            if (!tryEnter()) {
                return Mono.error(new RejectedExecutionException("canvas execution is shutting down"));
            }
            return mono.doFinally(signal -> exit());
        });
    }

    boolean tryEnter() {
        if (!accepting.get()) {
            return false;
        }
        inFlight.incrementAndGet();
        if (!accepting.get()) {
            exit();
            return false;
        }
        return true;
    }

    void exit() {
        int remaining = inFlight.updateAndGet(current -> Math.max(0, current - 1));
        if (remaining == 0) {
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }
    }

    public boolean isAccepting() {
        return accepting.get();
    }

    public int inFlightCount() {
        return inFlight.get();
    }

    @PreDestroy
    public void shutdown() {
        if (!accepting.getAndSet(false)) {
            return;
        }
        awaitDrain();
    }

    private void awaitDrain() {
        long deadline = System.nanoTime() + drainTimeout.toNanos();
        synchronized (monitor) {
            while (inFlight.get() > 0) {
                long remainingNanos = deadline - System.nanoTime();
                if (remainingNanos <= 0) {
                    log.warn("[LIFECYCLE] shutdown drain timed out, inFlight={}", inFlight.get());
                    return;
                }
                try {
                    long waitMillis = Math.max(1, Math.min(100, remainingNanos / 1_000_000));
                    monitor.wait(waitMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
