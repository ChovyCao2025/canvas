package org.chovy.canvas.engine.trace;

import org.chovy.canvas.config.CorrelationIdWebFilter;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.infrastructure.observability.MdcTaskDecorator;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * MDC scope for one canvas execution or one node execution.
 */
public record ExecutionTraceContext(
        String executionId,
        Long canvasId,
        String nodeId,
        String correlationId
) {

    public static final String EXECUTION_ID_KEY = "executionId";
    public static final String CANVAS_ID_KEY = "canvasId";
    public static final String NODE_ID_KEY = "nodeId";
    public static final String CORRELATION_ID_KEY = CorrelationIdWebFilter.MDC_KEY;

    public static ExecutionTraceContext from(ExecutionContext ctx) {
        return from(ctx, null);
    }

    public static ExecutionTraceContext from(ExecutionContext ctx, String nodeId) {
        Objects.requireNonNull(ctx, "ctx must not be null");
        return new ExecutionTraceContext(
                ctx.getExecutionId(),
                ctx.getCanvasId(),
                nodeId,
                CorrelationIdWebFilter.currentTraceId()
                        .orElseGet(() -> triggerPayloadTraceId(ctx)));
    }

    public Scope open() {
        Map<String, String> previous = MdcTaskDecorator.capture();
        asMdcMap().forEach(MDC::put);
        return new Scope(previous);
    }

    public Runnable wrap(Runnable task) {
        Objects.requireNonNull(task, "task must not be null");
        return () -> {
            try (Scope ignored = open()) {
                task.run();
            }
        };
    }

    public <T> Callable<T> wrap(Callable<T> task) {
        Objects.requireNonNull(task, "task must not be null");
        return () -> {
            try (Scope ignored = open()) {
                return task.call();
            }
        };
    }

    public <T> Mono<T> scope(Mono<T> source) {
        Objects.requireNonNull(source, "source must not be null");
        return Mono.defer(() -> {
            Scope scope = open();
            return source.doFinally(ignored -> scope.close());
        });
    }

    public Map<String, String> asMdcMap() {
        Map<String, String> values = new LinkedHashMap<>();
        putIfPresent(values, EXECUTION_ID_KEY, executionId);
        if (canvasId != null) {
            values.put(CANVAS_ID_KEY, String.valueOf(canvasId));
        }
        putIfPresent(values, NODE_ID_KEY, nodeId);
        putIfPresent(values, CORRELATION_ID_KEY, correlationId);
        return values;
    }

    private static void putIfPresent(Map<String, String> values, String key, String value) {
        if (value != null && !value.isBlank()) {
            values.put(key, value);
        }
    }

    private static String triggerPayloadTraceId(ExecutionContext ctx) {
        Object value = ctx.getTriggerPayload().get(CORRELATION_ID_KEY);
        if (value == null) {
            return null;
        }
        String traceId = String.valueOf(value);
        return traceId.isBlank() ? null : traceId;
    }

    public static final class Scope implements AutoCloseable {
        private final Map<String, String> previous;
        private boolean closed;

        private Scope(Map<String, String> previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            MdcTaskDecorator.restore(previous);
        }
    }
}
