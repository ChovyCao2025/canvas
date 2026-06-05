package org.chovy.canvas.engine.lifecycle;

import jakarta.annotation.PreDestroy;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central admission gate for canvas execution work.
 */
@Component
public class ExecutionLifecycleGate implements SmartLifecycle {

    private final AtomicBoolean acceptingNewWork = new AtomicBoolean(true);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicInteger inFlight = new AtomicInteger();

    public WorkPermit acquire(String source) {
        ensureAccepting(source);
        inFlight.incrementAndGet();
        if (!acceptingNewWork.get()) {
            release();
            throw new ExecutionLifecycleException(source);
        }
        return new WorkPermit();
    }

    public void ensureAccepting(String source) {
        if (!acceptingNewWork.get()) {
            throw new ExecutionLifecycleException(source);
        }
    }

    public <T> Mono<T> guard(String source, Mono<T> work) {
        return Mono.defer(() -> {
            WorkPermit permit = acquire(source);
            return work.doFinally(signalType -> permit.close());
        });
    }

    public <T> Mono<T> trackAccepted(String source, Mono<T> work) {
        return Mono.defer(() -> {
            WorkPermit permit = acquireAccepted();
            return work.doFinally(signalType -> permit.close());
        });
    }

    public boolean isAcceptingNewWork() {
        return acceptingNewWork.get();
    }

    public int inFlightCount() {
        return inFlight.get();
    }

    @PreDestroy
    public void beginShutdown() {
        acceptingNewWork.set(false);
        running.set(false);
    }

    @Override
    public void start() {
        acceptingNewWork.set(true);
        running.set(true);
    }

    @Override
    public void stop() {
        beginShutdown();
    }

    @Override
    public void stop(Runnable callback) {
        beginShutdown();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    private void release() {
        inFlight.updateAndGet(current -> current > 0 ? current - 1 : 0);
    }

    private WorkPermit acquireAccepted() {
        inFlight.incrementAndGet();
        return new WorkPermit();
    }

    public final class WorkPermit implements AutoCloseable {
        private final AtomicBoolean closed = new AtomicBoolean(false);

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                release();
            }
        }
    }
}
