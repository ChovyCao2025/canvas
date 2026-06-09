package org.chovy.canvas.engine.lifecycle;

/**
 * Raised when a new execution trigger is submitted after shutdown admission is closed.
 */
public class ExecutionLifecycleException extends IllegalStateException {

    /**
     * 创建 ExecutionLifecycleException 实例并注入 engine.lifecycle 场景依赖。
     * @param source source 参数，用于 ExecutionLifecycleException 流程中的校验、计算或对象转换。
     */
    public ExecutionLifecycleException(String source) {
        super("Execution lifecycle is stopping; rejecting new trigger work: " + source);
    }
}
