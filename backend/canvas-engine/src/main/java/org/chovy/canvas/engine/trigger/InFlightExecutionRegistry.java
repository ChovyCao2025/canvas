package org.chovy.canvas.engine.trigger;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 正在执行中的画布执行实例注册表。
 * 用于 Kill Switch（FORCE 模式）取消正在进行的 Reactor 订阅。
 */
@Slf4j
@Component
public class InFlightExecutionRegistry {

    /** canvasId → { executionId → Disposable } */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, Disposable>> registry =
            new ConcurrentHashMap<>();

    /** 注册一个正在执行的实例 */
    public void register(Long canvasId, String executionId, Disposable disposable) {
        registry.computeIfAbsent(canvasId, k -> new ConcurrentHashMap<>())
                .put(executionId, disposable);
        log.debug("[REGISTRY] 注册执行 canvasId={} executionId={}", canvasId, executionId);
    }

    /** 执行结束时注销 */
    public void deregister(Long canvasId, String executionId) {
        ConcurrentHashMap<String, Disposable> map = registry.get(canvasId);
        if (map != null) {
            map.remove(executionId);
            if (map.isEmpty()) registry.remove(canvasId);
        }
    }

    /**
     * 取消指定画布的所有正在进行的执行（FORCE Kill）。
     * 调用 Reactor Disposable.dispose() 触发 Mono 取消信号。
     */
    public int cancelAll(Long canvasId) {
        ConcurrentHashMap<String, Disposable> map = registry.remove(canvasId);
        if (map == null) return 0;
        map.forEach((execId, d) -> {
            if (!d.isDisposed()) {
                d.dispose();
                log.info("[REGISTRY] FORCE 取消执行 canvasId={} executionId={}", canvasId, execId);
            }
        });
        return map.size();
    }

    public int activeCount(Long canvasId) {
        ConcurrentHashMap<String, Disposable> map = registry.get(canvasId);
        return map == null ? 0 : map.size();
    }
}
