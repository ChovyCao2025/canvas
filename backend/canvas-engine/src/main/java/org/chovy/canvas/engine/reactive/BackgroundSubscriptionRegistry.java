package org.chovy.canvas.engine.reactive;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Tracks fire-and-forget Reactor subscriptions so they have a lifecycle owner.
 */
@Slf4j
@Component
public class BackgroundSubscriptionRegistry {

    private final Set<Disposable> active = ConcurrentHashMap.newKeySet();
    private final Object drainMonitor = new Object();
    private final Duration drainTimeout;

    public BackgroundSubscriptionRegistry() {
        this(Duration.ofSeconds(30));
    }

    @Autowired
    public BackgroundSubscriptionRegistry(
            @Value("${canvas.shutdown.background-subscription-drain-timeout-ms:30000}") long drainTimeoutMs) {
        this(Duration.ofMillis(Math.max(0, drainTimeoutMs)));
    }

    BackgroundSubscriptionRegistry(Duration drainTimeout) {
        this.drainTimeout = drainTimeout == null ? Duration.ZERO : drainTimeout;
    }

    /**
     * Subscribe to a background Mono and remove it from the active set after termination.
     */
    public Disposable track(String name, Mono<?> source, Consumer<Throwable> onError) {
        return track(name, (Publisher<?>) source, onError);
    }

    /**
     * Subscribe to a background Publisher and remove it from the active set after termination.
     */
    public Disposable track(String name, Publisher<?> source, Consumer<Throwable> onError) {
        Disposable.Swap slot = Disposables.swap();
        active.add(slot);
        Disposable subscription = Flux.from(source)
                .doFinally(signalType -> remove(slot))
                .subscribe(ignored -> {
                }, error -> {
                    if (onError != null) {
                        onError.accept(error);
                    } else {
                        log.warn("[BACKGROUND] task failed name={}: {}", name, error.getMessage(), error);
                    }
                });
        slot.update(subscription);
        if (slot.isDisposed()) {
            remove(slot);
        }
        return slot;
    }

    int activeCount() {
        return active.size();
    }

    @PreDestroy
    public void shutdown() {
        waitForDrain();
        for (Disposable disposable : active) {
            disposable.dispose();
        }
        active.clear();
        signalDrainWaiters();
    }

    private void remove(Disposable disposable) {
        if (active.remove(disposable)) {
            signalDrainWaiters();
        }
    }

    private void waitForDrain() {
        long timeoutNanos = drainTimeout.toNanos();
        if (timeoutNanos <= 0) {
            return;
        }
        long deadline = System.nanoTime() + timeoutNanos;
        synchronized (drainMonitor) {
            while (!active.isEmpty()) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return;
                }
                try {
                    TimeUnit.NANOSECONDS.timedWait(drainMonitor, remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void signalDrainWaiters() {
        synchronized (drainMonitor) {
            drainMonitor.notifyAll();
        }
    }
}
