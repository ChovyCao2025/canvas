package org.chovy.canvas.engine.expression;

import java.util.Map;

/**
 * 表达式编译和执行的本地契约。
 *
 * <p>实现类负责供应商相关的沙箱、超时、结果大小限制和编译缓存协作。
 */
public interface ExpressionEngine {

    /**
     * 预编译指定节点的表达式脚本。
     *
     * @param canvasId 画布 ID
     * @param nodeId 节点 ID
     * @param code 脚本源码
     */
    void precompile(Long canvasId, String nodeId, String code);

    /**
     * 执行指定节点脚本并返回输出变量。
     *
     * @param canvasId 画布 ID
     * @param nodeId 节点 ID
     * @param code 脚本源码
     * @param variables 输入变量
     * @return 脚本执行后的输出变量
     * @throws ExpressionException 表达式编译、沙箱校验或执行失败时抛出
     */
    Map<String, Object> execute(Long canvasId, String nodeId, String code, Map<String, Object> variables)
            throws ExpressionException;

    /**
     * 执行单条表达式并返回原始结果。
     *
     * @param expression 表达式源码
     * @param variables 输入变量
     * @return 表达式执行结果
     * @throws ExpressionException 表达式执行失败时抛出
     */
    Object evaluate(String expression, Map<String, Object> variables) throws ExpressionException;

    /**
     * 清理指定画布关联的表达式编译缓存。
     *
     * @param canvasId 画布 ID
     */
    void evictCanvas(Long canvasId);

    /**
     * 表达式引擎异常。
     */
    class ExpressionException extends Exception {
        /**
         * 创建 ExpressionException 实例并注入 engine.expression 场景依赖。
         * @param message 原因或消息文本，用于记录状态变化的业务依据。
         */
        public ExpressionException(String message) {
            super(message);
        }

        /**
         * 创建 ExpressionException 实例并注入 engine.expression 场景依赖。
         * @param message 原因或消息文本，用于记录状态变化的业务依据。
         * @param cause cause 参数，用于 ExpressionException 流程中的校验、计算或对象转换。
         */
        public ExpressionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
