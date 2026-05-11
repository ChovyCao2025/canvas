package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;

import java.util.List;
import java.util.Map;

/**
 * 集线器：等待所有上游完成（SUCCESS + FAILED 均计入），再继续执行下游。
 * timeout 配置防止并行分支永久阻塞。
 */
@NodeHandlerType("HUB")
public class HubHandler implements NodeHandler {

    @Override
    public NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
        String nextNodeId = (String) config.get("nextNodeId");
        return NodeResult.ok(nextNodeId, Map.of());
    }

    /**
     * 检查所有上游是否都已"完成"（SUCCESS / FAILED / SKIPPED）。
     * 与 LogicRelationHandler 不同，Hub 不关心结果，只关心是否"done"。
     */
    public static boolean allUpstreamDone(List<String> upstreamIds, ExecutionContext ctx) {
        if (upstreamIds == null || upstreamIds.isEmpty()) return true;
        return upstreamIds.stream().allMatch(ctx::isNodeDone);
    }

    /** 获取 timeout 秒数（默认 600s） */
    public static int getTimeoutSeconds(Map<String, Object> config) {
        Object t = config.get("timeout");
        if (t instanceof Number n) return n.intValue();
        return 600;
    }
}
