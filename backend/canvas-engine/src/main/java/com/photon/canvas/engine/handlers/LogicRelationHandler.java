package com.photon.canvas.engine.handlers;

import com.photon.canvas.engine.context.ExecutionContext;
import com.photon.canvas.engine.context.NodeStatus;
import com.photon.canvas.engine.handler.NodeHandler;
import com.photon.canvas.engine.handler.NodeHandlerType;
import com.photon.canvas.engine.handler.NodeResult;

import java.util.List;
import java.util.Map;

/**
 * 逻辑关系节点：
 * AND — 所有上游节点 SUCCESS 时继续；
 * OR  — 任意上游节点 SUCCESS 时继续。
 * 在调度器层检查（checkUpstreamCondition），此 Handler 仅负责确定 nextNodeId。
 */
@NodeHandlerType("LOGIC_RELATION")
public class LogicRelationHandler implements NodeHandler {

    @Override
    public NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
        String nextNodeId = (String) config.get("nextNodeId");
        return NodeResult.ok(nextNodeId, Map.of());
    }

    /**
     * 检查上游是否满足触发条件（由调度器在进入节点前调用）。
     *
     * @param relation  "AND" | "OR"
     * @param upstreamIds 直接上游节点 ID 列表
     * @param ctx        执行上下文
     * @return true=可继续执行；false=条件未满足，需等待
     */
    public static boolean checkCondition(String relation, List<String> upstreamIds, ExecutionContext ctx) {
        if (upstreamIds == null || upstreamIds.isEmpty()) return true;

        if ("OR".equals(relation)) {
            return upstreamIds.stream()
                    .anyMatch(id -> ctx.getNodeStatus(id) == NodeStatus.SUCCESS);
        }
        // AND：所有上游 SUCCESS；任意 SKIPPED 或 FAILED 则立即失败
        for (String id : upstreamIds) {
            NodeStatus s = ctx.getNodeStatus(id);
            if (s == NodeStatus.FAILED || s == NodeStatus.SKIPPED) return false;
            if (s != NodeStatus.SUCCESS) return false; // 还未完成
        }
        return true;
    }

    /**
     * 判断 AND 模式下是否应立即 FAILED（上游有 FAILED 或 SKIPPED）
     */
    public static boolean shouldFailImmediately(String relation, List<String> upstreamIds, ExecutionContext ctx) {
        if (!"AND".equals(relation)) return false;
        return upstreamIds.stream().anyMatch(id -> {
            NodeStatus s = ctx.getNodeStatus(id);
            return s == NodeStatus.FAILED || s == NodeStatus.SKIPPED;
        });
    }
}
