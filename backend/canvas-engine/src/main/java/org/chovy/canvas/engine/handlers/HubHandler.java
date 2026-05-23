package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 集线器：等待所有上游完成（SUCCESS + FAILED 均计入），再继续执行下游。
 * timeout 配置防止并行分支永久阻塞。
 *
 * 说明：
 * - Hub 关注“是否完成”，不关注“成功/失败结果值”；
 * - 真正的等待与超时控制在 DagEngine 调度层实现。
 */
@Component
@NodeHandlerType("HUB")
public class HubHandler implements NodeHandler {

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // HUB 的等待逻辑在调度器层，handler 只负责“通过后去哪”
        String nextNodeId = (String) config.get("nextNodeId");
        // 无额外输出，纯路由型节点
        return Mono.just(NodeResult.ok(nextNodeId, Map.of()));
    }

    /**
     * 检查所有上游是否都已"完成"（SUCCESS / FAILED / SKIPPED）。
     * 与 LogicRelationHandler 不同，Hub 不关心结果，只关心是否"done"。
     */
    public static boolean allUpstreamDone(List<String> upstreamIds, ExecutionContext ctx) {
        if (upstreamIds == null || upstreamIds.isEmpty()) return true;
        // isNodeDone 把 SUCCESS/FAILED/SKIPPED 都视作“完成”
        return upstreamIds.stream().allMatch(ctx::isNodeDone);
    }

    /** 获取 timeout 秒数（默认 600s） */
    public static int getTimeoutSeconds(Map<String, Object> config) {
        Object t = config.get("timeout");
        if (t instanceof Number n) return n.intValue();
        return 600;
    }
}
