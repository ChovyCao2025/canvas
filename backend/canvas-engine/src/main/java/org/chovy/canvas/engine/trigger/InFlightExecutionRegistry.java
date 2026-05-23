package org.chovy.canvas.engine.trigger;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.Disposables;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 正在执行中的画布执行实例注册表。
 * 用于 Kill Switch（FORCE 模式）取消正在进行的 Reactor 订阅。
 */
@Slf4j
@Component
public class InFlightExecutionRegistry {

    /** canvasId → { executionId → cancellable subscription slot } */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, Disposable.Swap>> registry =
            new ConcurrentHashMap<>();
    private final AtomicInteger totalActive = new AtomicInteger(0);

    /**
     * 原子准入一个执行实例。
     *
     * <p>调用方先拿到 slot 占位，真正订阅 Reactor 链路后再把 subscription cancellation
     * 更新进 slot。这样可以在执行启动前完成并发准入，同时 FORCE kill 仍能取消真实订阅。</p>
     */
    public Optional<Disposable.Swap> tryAcquire(Long canvasId, String executionId,
                                                int canvasLimit, int globalLimit) {
        if (canvasLimit <= 0 || globalLimit <= 0) {
            return Optional.empty();
        }

        int activeAfterIncrement = totalActive.incrementAndGet();
        if (activeAfterIncrement > globalLimit) {
            totalActive.decrementAndGet();
            return Optional.empty();
        }

        AtomicBoolean registered = new AtomicBoolean(false);
        Disposable.Swap slot = Disposables.swap();
        registry.compute(canvasId, (id, current) -> {
            ConcurrentHashMap<String, Disposable.Swap> map =
                    current != null ? current : new ConcurrentHashMap<>();
            if (map.size() >= canvasLimit) {
                return map;
            }
            map.put(executionId, slot);
            registered.set(true);
            return map;
        });

        if (!registered.get()) {
            totalActive.decrementAndGet();
            return Optional.empty();
        }

        log.debug("[REGISTRY] 准入执行 canvasId={} executionId={}", canvasId, executionId);
        return Optional.of(slot);
    }

    /** 执行结束时注销 */
    public void deregister(Long canvasId, String executionId) {
        ConcurrentHashMap<String, Disposable.Swap> map = registry.get(canvasId);
        if (map != null) {
            Disposable.Swap removed = map.remove(executionId);
            if (removed != null) {
                totalActive.updateAndGet(v -> Math.max(0, v - 1));
            }
            if (map.isEmpty()) registry.remove(canvasId);
        }
    }

    /**
     * 取消指定画布的所有正在进行的执行（FORCE Kill）。
     * 调用 Reactor Disposable.dispose() 触发 Mono 取消信号。
     */
    public int cancelAll(Long canvasId) {
        ConcurrentHashMap<String, Disposable.Swap> map = registry.remove(canvasId);
        if (map == null) return 0;
        map.forEach((execId, d) -> {
            if (!d.isDisposed()) {
                d.dispose();
                log.info("[REGISTRY] FORCE 取消执行 canvasId={} executionId={}", canvasId, execId);
            }
        });
        totalActive.updateAndGet(v -> Math.max(0, v - map.size()));
        return map.size();
    }

    public int activeCount(Long canvasId) {
        ConcurrentHashMap<String, Disposable.Swap> map = registry.get(canvasId);
        return map == null ? 0 : map.size();
    }

    public int totalActiveCount() {
        return totalActive.get();
    }
}
