package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
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
 * 逻辑关系节点：
 * AND — 所有上游节点 SUCCESS 时继续；
 * OR  — 任意上游节点 SUCCESS 时继续。
 * 在调度器层检查（checkUpstreamCondition），此 Handler 仅负责确定 nextNodeId。
 */
@Component
@NodeHandlerType("LOGIC_RELATION")
public class LogicRelationHandler implements NodeHandler {

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // 关系判断已在调度器完成，这里只负责输出下一跳
        String nextNodeId = (String) config.get(MapFieldKeys.NEXT_NODE_ID);
        return Mono.just(NodeResult.ok(nextNodeId, Map.of()));
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
            // OR：任意上游成功即可放行
            return upstreamIds.stream()
                    .anyMatch(id -> ctx.getNodeStatus(id) == NodeStatus.SUCCESS);
        }
        // AND：所有上游 SUCCESS；任意终态非成功则立即失败
        for (String id : upstreamIds) {
            NodeStatus s = ctx.getNodeStatus(id);
            if (isTerminalNonSuccess(s)) return false;
            if (s != NodeStatus.SUCCESS) return false; // 还未完成
        }
        return true;
    }

    /**
     * 判断 AND 模式下是否应立即 FAILED（上游有终态非成功）
     */
    public static boolean shouldFailImmediately(String relation, List<String> upstreamIds, ExecutionContext ctx) {
        if (!"AND".equals(relation)) return false;
        return upstreamIds.stream().anyMatch(id -> {
            NodeStatus s = ctx.getNodeStatus(id);
            return isTerminalNonSuccess(s);
        });
    }

    private static boolean isTerminalNonSuccess(NodeStatus status) {
        return status == NodeStatus.FAILED
                || status == NodeStatus.TIMEOUT
                || status == NodeStatus.SUPPRESSED
                || status == NodeStatus.SKIPPED
                || status == NodeStatus.PARTIAL_FAIL;
    }
}
