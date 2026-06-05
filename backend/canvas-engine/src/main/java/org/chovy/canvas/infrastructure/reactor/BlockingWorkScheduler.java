package org.chovy.canvas.infrastructure.reactor;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Central adapter for blocking work used from the reactive application.
 */
@Component
public class BlockingWorkScheduler {

    public <T> Mono<T> call(String operation, Callable<T> callable) {
        Objects.requireNonNull(callable, "callable must not be null");
        return Mono.fromCallable(() -> {
                    assertNotEventLoop(operation);
                    return callable.call();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> run(String operation, ThrowingRunnable runnable) {
        Objects.requireNonNull(runnable, "runnable must not be null");
        return call(operation, () -> {
            runnable.run();
            return null;
        }).then();
    }

    public <T> T await(String operation, Mono<T> mono) {
        Objects.requireNonNull(mono, "mono must not be null");
        assertNotEventLoop(operation);
        return mono.block();
    }

    public <T> T get(String operation, Callable<T> callable) {
        assertNotEventLoop(operation);
        return call(operation, callable).block();
    }

    private void assertNotEventLoop(String operation) {
        String threadName = Thread.currentThread().getName();
        String lower = threadName.toLowerCase(Locale.ROOT);
        if (lower.contains("reactor-http")
                || lower.contains("reactor-tcp")
                || lower.contains("nioeventloop")
                || lower.contains("eventloop")) {
            String name = operation == null || operation.isBlank() ? "blocking work" : operation;
            throw new IllegalStateException(name + " cannot run on event-loop thread: " + threadName);
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
