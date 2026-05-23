package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 结束节点：流程图出口标记，终止节点（is_terminal=1），无出边，无额外逻辑。
 *
 * <p>调度器收到 terminal 结果后会结束当前 execution，
 * 并把上下文汇总结果返回给上层调用方（如直调场景）。
 * 该节点不会再触发任何后继节点。
 */
@Component
@NodeHandlerType("END")
public class EndHandler implements NodeHandler {

    /**
     * 返回 terminal 结果，通知调度器结束本次流程。
     *
     * <p>此处不会再有下游调度行为。
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // END 节点无输出配置，统一返回空 output
        // terminal 结果会触发调度器收敛执行并结束 DAG
        // 若上游需要返回业务数据，应在 END 前通过 DIRECT_RETURN 组装
        // END 不区分触发来源（MQ/事件/直调），终止语义一致
        return Mono.just(NodeResult.terminal(Map.of()));
    }

    @Override public boolean isBenefitNode() { return false; }
    @Override public boolean isReachNode()   { return false; }
}
