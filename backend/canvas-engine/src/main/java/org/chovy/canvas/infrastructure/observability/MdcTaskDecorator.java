package org.chovy.canvas.infrastructure.observability;

import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Captures the caller MDC map and restores it around bounded asynchronous work.
 */
public final class MdcTaskDecorator {

    private MdcTaskDecorator() {
    }

    public static Runnable decorate(Runnable task) {
        Objects.requireNonNull(task, "task must not be null");
        Map<String, String> captured = copy(MDC.getCopyOfContextMap());
        return () -> {
            Map<String, String> previous = copy(MDC.getCopyOfContextMap());
            restore(captured);
            try {
                task.run();
            } finally {
                restore(previous);
            }
        };
    }

    public static <T> Callable<T> decorate(Callable<T> task) {
        Objects.requireNonNull(task, "task must not be null");
        Map<String, String> captured = copy(MDC.getCopyOfContextMap());
        return () -> {
            Map<String, String> previous = copy(MDC.getCopyOfContextMap());
            restore(captured);
            try {
                return task.call();
            } finally {
                restore(previous);
            }
        };
    }

    public static Map<String, String> capture() {
        return copy(MDC.getCopyOfContextMap());
    }

    public static void restore(Map<String, String> contextMap) {
        if (contextMap == null || contextMap.isEmpty()) {
            MDC.clear();
            return;
        }
        MDC.setContextMap(contextMap);
    }

    private static Map<String, String> copy(Map<String, String> contextMap) {
        if (contextMap == null || contextMap.isEmpty()) {
            return null;
        }
        return new HashMap<>(contextMap);
    }
}
