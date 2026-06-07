package org.chovy.canvas.infrastructure.reactor;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 * Tracks fire-and-forget reactive tasks so lifecycle shutdown can dispose them.
 */
@Slf4j
@Component
public class TrackedReactiveTaskRegistry {

    private static final TrackedReactiveTaskRegistry DIRECT =
            new TrackedReactiveTaskRegistry(Duration.ZERO, false);

    private final Set<TrackedTask> tasks = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Duration shutdownTimeout;
    private final boolean closeOnShutdown;

    @Autowired
    public TrackedReactiveTaskRegistry(
            @Value("${canvas.reactive-tasks.shutdown-timeout-ms:5000}") long shutdownTimeoutMs) {
        this(Duration.ofMillis(Math.max(0L, shutdownTimeoutMs)), true);
    }

    TrackedReactiveTaskRegistry(Duration shutdownTimeout, boolean closeOnShutdown) {
        this.shutdownTimeout = shutdownTimeout == null ? Duration.ZERO : shutdownTimeout;
        this.closeOnShutdown = closeOnShutdown;
    }

    public static TrackedReactiveTaskRegistry direct() {
        return DIRECT;
    }

    public Disposable submit(String taskName, Mono<?> task) {
        return submit(taskName, task, null);
    }

    public Disposable submit(String taskName, Mono<?> task, Consumer<Throwable> onError) {
        return submitPublisher(taskName, task, onError);
    }

    public Disposable submit(String taskName, Publisher<?> task, Consumer<Throwable> onError) {
        return submitPublisher(taskName, task, onError);
    }

    private Disposable submitPublisher(String taskName, Publisher<?> task, Consumer<Throwable> onError) {
        Objects.requireNonNull(task, "task must not be null");
        String resolvedTaskName = taskName == null || taskName.isBlank() ? "reactive-task" : taskName;
        if (closed.get()) {
            throw new RejectedExecutionException("reactive task registry is closed: " + resolvedTaskName);
        }

        Disposable.Swap delegate = Disposables.swap();
        TrackedTask tracked = new TrackedTask(resolvedTaskName, delegate);
        tasks.add(tracked);
        try {
            Disposable subscribed = Flux.from(task)
                    .doFinally(signalType -> tasks.remove(tracked))
                    .subscribe(
                            ignored -> {
                            },
                            e -> handleError(resolvedTaskName, e, onError));
            delegate.update(subscribed);
            return tracked;
        } catch (RuntimeException e) {
            tasks.remove(tracked);
            delegate.dispose();
            throw e;
        }
    }

    public int inFlightTaskCount() {
        return tasks.size();
    }

    public boolean isClosed() {
        return closed.get();
    }

    public boolean drain(Duration timeout) {
        long waitNanos = timeout == null ? 0L : Math.max(0L, timeout.toNanos());
        long deadline = System.nanoTime() + waitNanos;
        while (!tasks.isEmpty() && System.nanoTime() < deadline) {
            LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return tasks.isEmpty();
    }

    @PreDestroy
    public void shutdown() {
        if (closeOnShutdown && !closed.compareAndSet(false, true)) {
            return;
        }
        if (!drain(shutdownTimeout)) {
            tasks.forEach(TrackedTask::dispose);
            tasks.clear();
        }
    }

    private void handleError(String taskName, Throwable error, Consumer<Throwable> onError) {
        if (onError != null) {
            onError.accept(error);
            return;
        }
        log.warn("[REACTIVE_TASK] task failed taskName={}: {}", taskName, error.getMessage(), error);
    }

    private record TrackedTask(String name, Disposable.Swap delegate) implements Disposable {
        @Override
        public void dispose() {
            delegate.dispose();
        }

        @Override
        public boolean isDisposed() {
            return delegate.isDisposed();
        }
    }
}
