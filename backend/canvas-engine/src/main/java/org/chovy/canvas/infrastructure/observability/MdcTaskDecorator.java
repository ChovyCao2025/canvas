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

    /**
     * 执行 MdcTaskDecorator 流程，围绕 mdc task decorator 完成校验、计算或结果组装。
     */
    private MdcTaskDecorator() {
    }

    /**
     * decorate 处理 infrastructure.observability 场景的业务逻辑。
     * @param task task 参数，用于 decorate 流程中的校验、计算或对象转换。
     * @return 返回 decorate 流程生成的业务结果。
     */
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

    /**
     * decorate 处理 infrastructure.observability 场景的业务逻辑。
     * @param task task 参数，用于 decorate 流程中的校验、计算或对象转换。
     * @return 返回 decorate 流程生成的业务结果。
     */
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

    /**
     * capture 处理 infrastructure.observability 场景的业务逻辑。
     * @return 返回 capture 生成的文本或业务键。
     */
    public static Map<String, String> capture() {
        return copy(MDC.getCopyOfContextMap());
    }

    /**
     * restore 更新 infrastructure.observability 场景的业务状态。
     * @param contextMap context map 参数，用于 restore 流程中的校验、计算或对象转换。
     */
    public static void restore(Map<String, String> contextMap) {
        if (contextMap == null || contextMap.isEmpty()) {
            MDC.clear();
            return;
        }
        MDC.setContextMap(contextMap);
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param String string 参数，用于 copy 流程中的校验、计算或对象转换。
     * @param contextMap context map 参数，用于 copy 流程中的校验、计算或对象转换。
     * @return 返回 copy 生成的文本或业务键。
     */
    private static Map<String, String> copy(Map<String, String> contextMap) {
        if (contextMap == null || contextMap.isEmpty()) {
            return null;
        }
        return new HashMap<>(contextMap);
    }
}
