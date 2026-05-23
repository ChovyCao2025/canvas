package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 开始节点：流程图入口标记，无实际逻辑，直接触发下游。
 * 触发器类型（is_trigger=1），无入边，无额外配置。
 *
 * <p>不同触发方式（事件/MQ/定时/直调）的触发载荷准备，
 * 都在进入 START 前由执行服务完成。
 * 因此该节点不做校验和转换。
 */
@Component
@NodeHandlerType("START")
public class StartHandler implements NodeHandler {

    /**
     * 直接透传到下游节点。
     *
     * <p>START 是编排锚点，不承担业务判断逻辑。
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // START 配置只关心 nextNodeId
        String nextNodeId = (String) config.get("nextNodeId");
        // 不产出额外 output，避免污染上下文字段
        // 若 nextNodeId 为空，调度器会在后续阶段识别为异常配置
        // START 本身不参与节点统计聚合，只作为流程入口标记
        return Mono.just(NodeResult.ok(nextNodeId, Map.of()));
    }

    @Override public boolean isBenefitNode() { return false; }
    @Override public boolean isReachNode()   { return false; }
}
