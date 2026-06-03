package org.chovy.canvas.engine.reactive;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Tracks fire-and-forget Reactor subscriptions so they have a lifecycle owner.
 */
@Slf4j
@Component
public class BackgroundSubscriptionRegistry {

    private final Set<Disposable> active = ConcurrentHashMap.newKeySet();

    /**
     * Subscribe to a background Mono and remove it from the active set after termination.
     */
    public Disposable track(String name, Mono<?> source, Consumer<Throwable> onError) {
        Disposable.Swap slot = Disposables.swap();
        active.add(slot);
        Disposable subscription = source
                .doFinally(signalType -> active.remove(slot))
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
            active.remove(slot);
        }
        return slot;
    }

    int activeCount() {
        return active.size();
    }

    @PreDestroy
    public void shutdown() {
        for (Disposable disposable : active) {
            disposable.dispose();
        }
        active.clear();
    }
}
