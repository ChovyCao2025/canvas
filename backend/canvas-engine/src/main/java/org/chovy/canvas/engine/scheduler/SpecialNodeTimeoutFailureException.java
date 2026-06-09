package org.chovy.canvas.engine.scheduler;

/** Fail-closed marker for special-node timeout terminal failures. */
public class SpecialNodeTimeoutFailureException extends IllegalStateException {

    /**
     * 创建 SpecialNodeTimeoutFailureException 实例并注入 engine.scheduler 场景依赖。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     */
    public SpecialNodeTimeoutFailureException(String message) {
        super(message);
    }

    /**
     * 创建 SpecialNodeTimeoutFailureException 实例并注入 engine.scheduler 场景依赖。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @param cause cause 参数，用于 SpecialNodeTimeoutFailureException 流程中的校验、计算或对象转换。
     */
    public SpecialNodeTimeoutFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * contains 处理 engine.scheduler 场景的业务逻辑。
     * @param error error 参数，用于 contains 流程中的校验、计算或对象转换。
     * @return 返回 contains 的布尔判断结果。
     */
    public static boolean contains(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SpecialNodeTimeoutFailureException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
