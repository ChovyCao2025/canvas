package org.chovy.canvas.execution.domain;

import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 定义 EndNodeHandler 的执行上下文数据结构或业务契约。
 */
@Component
@NodeHandlerType("END")
public class EndNodeHandler implements NodeHandler {

    /**
     * 执行 execute 对应的业务处理。
     * @param context context 参数
     * @return 处理后的结果
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        return NodeExecutionResult.success(Map.of("ended", true));
    }
}
