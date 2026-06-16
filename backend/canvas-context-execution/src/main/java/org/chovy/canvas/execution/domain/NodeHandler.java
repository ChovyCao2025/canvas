package org.chovy.canvas.execution.domain;

/**
 * 定义 NodeHandler 的执行上下文数据结构或业务契约。
 */
public interface NodeHandler {

    /**
     * 执行 execute 对应的业务处理。
     * @param context context 参数
     * @return 处理后的结果
     */
    NodeExecutionResult execute(NodeExecutionContext context);
}
