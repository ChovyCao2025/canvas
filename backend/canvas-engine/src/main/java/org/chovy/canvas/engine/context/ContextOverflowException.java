package org.chovy.canvas.engine.context;

/**
 * Raised when an execution context cannot accept more node output within its
 * in-memory safety limit.
 */
public class ContextOverflowException extends IllegalStateException {

    private final int currentSizeBytes;

    /**
     * 创建 ContextOverflowException 实例并注入 engine.context 场景依赖。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @param currentSizeBytes current size bytes 参数，用于 ContextOverflowException 流程中的校验、计算或对象转换。
     */
    public ContextOverflowException(String message, int currentSizeBytes) {
        super(message);
        this.currentSizeBytes = currentSizeBytes;
    }

    /**
     * getCurrentSizeBytes 查询 engine.context 场景的业务数据。
     * @return 返回 get current size bytes 计算得到的数量、金额或指标值。
     */
    public int getCurrentSizeBytes() {
        return currentSizeBytes;
    }
}
