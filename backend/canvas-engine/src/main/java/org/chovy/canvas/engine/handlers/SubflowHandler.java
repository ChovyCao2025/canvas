package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 子流程节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.SUBFLOW)
public class SubflowHandler implements NodeHandler {
    /** 子流程引用处理器，复用 SUB_FLOW_REF 的执行逻辑。 */
    private final SubFlowRefHandler delegate;

    /**
     * 构造 SubflowHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param delegate delegate 方法执行所需的业务参数
     */
    public SubflowHandler(SubFlowRefHandler delegate) {
        this.delegate = delegate;
    }

    /**
     * 执行当前节点或服务的核心处理流程。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        Map<String, Object> mapped = new HashMap<>(config);
        if (mapped.containsKey("subflowId") && !mapped.containsKey("subFlowId")) {
            mapped.put("subFlowId", mapped.get("subflowId"));
        }
        return delegate.executeAsync(mapped, ctx);
    }
}
