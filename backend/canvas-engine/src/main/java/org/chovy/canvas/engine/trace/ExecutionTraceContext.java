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

    /**
     * from 校验或转换 engine.trace 场景的数据。
     * @param ctx ctx 参数，用于 from 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    public static ExecutionTraceContext from(ExecutionContext ctx) {
        return from(ctx, null);
    }

    /**
     * from 校验或转换 engine.trace 场景的数据。
     * @param ctx ctx 参数，用于 from 流程中的校验、计算或对象转换。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @return 返回组装或转换后的结果对象。
     */
    public static ExecutionTraceContext from(ExecutionContext ctx, String nodeId) {
        Objects.requireNonNull(ctx, "ctx must not be null");
        return new ExecutionTraceContext(
                ctx.getExecutionId(),
                ctx.getCanvasId(),
                nodeId,
                CorrelationIdWebFilter.currentTraceId()
                        .orElseGet(() -> triggerPayloadTraceId(ctx)));
    }

    /**
     * open 处理 engine.trace 场景的业务逻辑。
     * @return 返回 open 流程生成的业务结果。
     */
    public Scope open() {
        Map<String, String> previous = MdcTaskDecorator.capture();
        asMdcMap().forEach(MDC::put);
        return new Scope(previous);
    }

    /**
     * wrap 处理 engine.trace 场景的业务逻辑。
     * @param task task 参数，用于 wrap 流程中的校验、计算或对象转换。
     * @return 返回 wrap 流程生成的业务结果。
     */
    public Runnable wrap(Runnable task) {
        Objects.requireNonNull(task, "task must not be null");
        return () -> {
            try (Scope ignored = open()) {
                task.run();
            }
        };
    }

    /**
     * wrap 处理 engine.trace 场景的业务逻辑。
     * @param task task 参数，用于 wrap 流程中的校验、计算或对象转换。
     * @return 返回 wrap 流程生成的业务结果。
     */
    public <T> Callable<T> wrap(Callable<T> task) {
        Objects.requireNonNull(task, "task must not be null");
        return () -> {
            try (Scope ignored = open()) {
                return task.call();
            }
        };
    }

    /**
     * scope 处理 engine.trace 场景的业务逻辑。
     * @param source source 参数，用于 scope 流程中的校验、计算或对象转换。
     * @return 返回 scope 流程生成的业务结果。
     */
    public <T> Mono<T> scope(Mono<T> source) {
        Objects.requireNonNull(source, "source must not be null");
        return Mono.defer(() -> {
            Scope scope = open();
            return source.doFinally(ignored -> scope.close());
        });
    }

    /**
     * asMdcMap 处理 engine.trace 场景的业务逻辑。
     * @return 返回 as mdc map 生成的文本或业务键。
     */
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

    /**
     * 非空时写入 MDC 值。
     *
     * @param values MDC 值映射
     * @param key MDC key
     * @param value MDC value
     */
    private static void putIfPresent(Map<String, String> values, String key, String value) {
        if (value != null && !value.isBlank()) {
            values.put(key, value);
        }
    }

    /**
     * 从触发载荷中读取 trace/correlation ID。
     *
     * @param ctx 执行上下文
     * @return trace ID，缺失时返回 null
     */
    private static String triggerPayloadTraceId(ExecutionContext ctx) {
        Object value = ctx.getTriggerPayload().get(CORRELATION_ID_KEY);
        if (value == null) {
            return null;
        }
        String traceId = String.valueOf(value);
        return traceId.isBlank() ? null : traceId;
    }

    /**
     * Scope 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public static final class Scope implements AutoCloseable {
        private final Map<String, String> previous;
        private boolean closed;

        /**
         * 创建 MDC 作用域并保存旧值。
         *
         * @param previous 进入作用域前的 MDC 快照
         */
        private Scope(Map<String, String> previous) {
            this.previous = previous;
        }

        /**
         * close 删除或清理 engine.trace 场景的业务数据。
         */
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
