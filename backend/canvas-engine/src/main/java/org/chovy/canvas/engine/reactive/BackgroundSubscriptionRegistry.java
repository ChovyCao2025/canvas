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
 * 跟踪即发即忘的 Reactor 订阅，确保它们具备生命周期所有者。
 */
@Slf4j
@Component
public class BackgroundSubscriptionRegistry {

    private final Set<Disposable> active = ConcurrentHashMap.newKeySet();
    private final Object drainMonitor = new Object();
    private final Duration drainTimeout;

    /**
     * 创建 BackgroundSubscriptionRegistry 实例并注入 engine.reactive 场景依赖。
     */
    public BackgroundSubscriptionRegistry() {
        this(Duration.ofSeconds(30));
    }

    /**
     * 创建 BackgroundSubscriptionRegistry 实例并注入 engine.reactive 场景依赖。
     * @param drainTimeoutMs 时间参数，用于计算窗口、过期或审计时间。
     */
    @Autowired
    public BackgroundSubscriptionRegistry(
            @Value("${canvas.shutdown.background-subscription-drain-timeout-ms:30000}") long drainTimeoutMs) {
        this(Duration.ofMillis(Math.max(0, drainTimeoutMs)));
    }

    /**
     * 使用指定排空超时时间创建后台订阅注册表。
     *
     * @param drainTimeout 关闭时等待订阅自然结束的最长时间
     */
    BackgroundSubscriptionRegistry(Duration drainTimeout) {
        this.drainTimeout = drainTimeout == null ? Duration.ZERO : drainTimeout;
    }

    /**
     * 订阅后台 Mono，并在终止后从活跃集合移除。
     *
     * @param name 后台任务名称
     * @param source 待订阅 Mono
     * @param onError 错误回调，可为空
     * @return 可取消的订阅句柄
     */
    public Disposable track(String name, Mono<?> source, Consumer<Throwable> onError) {
        return track(name, (Publisher<?>) source, onError);
    }

    /**
     * 订阅后台 Publisher，并在终止后从活跃集合移除。
     *
     * @param name 后台任务名称
     * @param source 待订阅 Publisher
     * @param onError 错误回调，可为空
     * @return 可取消的订阅句柄
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

    /**
     * 查询当前活跃后台订阅数。
     *
     * @return 活跃订阅数
     */
    int activeCount() {
        return active.size();
    }

    /**
     * shutdown 处理 engine.reactive 场景的业务逻辑。
     */
    @PreDestroy
    public void shutdown() {
        waitForDrain();
        for (Disposable disposable : active) {
            disposable.dispose();
        }
        active.clear();
        signalDrainWaiters();
    }

    /**
     * 从活跃集合移除订阅并唤醒排空等待方。
     *
     * @param disposable 待移除订阅
     */
    private void remove(Disposable disposable) {
        if (active.remove(disposable)) {
            signalDrainWaiters();
        }
    }

    /**
     * 等待活跃订阅在关闭前自然排空。
     */
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
                // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * 唤醒等待订阅排空的线程。
     */
    private void signalDrainWaiters() {
        synchronized (drainMonitor) {
            drainMonitor.notifyAll();
        }
    }
}
