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

    /**
     * 判断 is Terminal Non Success 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param status status 状态值或状态筛选条件
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private static boolean isTerminalNonSuccess(NodeStatus status) {
        return status == NodeStatus.FAILED
                || status == NodeStatus.TIMEOUT
                || status == NodeStatus.SUPPRESSED
                || status == NodeStatus.SKIPPED
                || status == NodeStatus.PARTIAL_FAIL;
    }
}
