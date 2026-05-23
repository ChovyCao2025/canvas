package org.chovy.canvas.engine.handler;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 节点执行结果模型。
 * 约定：一个节点可返回“单路下一跳”“多路分支”“终止”“失败”“等待（阈值节点）”等形态。
 */
public record NodeResult(
        String nextNodeId,
        String successNodeId,
        String failNodeId,
        String elseNodeId,
        Map<String, String> branchMap,
        Map<String, Object> output,
        boolean success,
        String errorMessage,
        boolean pending,               // true=阈值未满足，挂起等待更多上游完成（THRESHOLD 节点专用）
        NodeOutcome outcome,
        Map<String, String> routes,
        String reasonCode,
        String reasonMessage,
        Long resumeAtEpochMs
) {
    // 说明：
    // - nextNodeId 与 success/fail/branchMap 互斥使用；
    // - output 会合并到 ExecutionContext.flatContext（由调度层处理）。

    /** 普通单路成功（nextNodeId）。 */
    public static NodeResult ok(String nextNodeId, Map<String, Object> output) {
        return new NodeResult(nextNodeId, null, null, null, null, output, true, null, false,
                NodeOutcome.SUCCESS, route("success", nextNodeId), null, null, null);
    }
    /** 正常终止（无下一跳）。 */
    public static NodeResult terminal(Map<String, Object> output) {
        return new NodeResult(null, null, null, null, null, output, true, null, false,
                NodeOutcome.SUCCESS, Map.of(), null, null, null);
    }
    /** IF 类节点的二路分支结果。 */
    public static NodeResult ifResult(boolean condition, String successId, String failId) {
        return new NodeResult(null, condition ? successId : null, !condition ? failId : null,
                null, null, Map.of(), true, null, false,
                NodeOutcome.SUCCESS, route(condition ? "success" : "fail", condition ? successId : failId),
                null, null, null);
    }
    /** 执行失败。 */
    public static NodeResult fail(String errorMessage) {
        return new NodeResult(null, null, null, null, null, Map.of(), false, errorMessage, false,
                NodeOutcome.FAIL, Map.of(), "NODE_FAILED", errorMessage, null);
    }
    /** 多分支节点（如 PRIORITY / AB_SPLIT / SELECTOR）。 */
    public static NodeResult multiNext(Map<String, String> branchMap, String elseNodeId) {
        // branchMap key 仅作为分支标签，value 才是实际节点 ID
        return new NodeResult(null, null, null, elseNodeId, branchMap, Map.of(), true, null, false,
                NodeOutcome.SUCCESS, routes(branchMap, elseNodeId), null, null, null);
    }

    /**
     * 阈值未满足，挂起等待更多上游完成（THRESHOLD 节点专用）。
     * 以 {@code success=true, pending=true} 实现，使其通过 {@code executeHandlerWithRepeat}
     * 的 repeat 检查路径——持锁期间到来的上游信号通过 repeatPending 被捕获，
     * repeat 重新评估时若阈值满足则正确路由。
     */
    public static NodeResult waiting() {
        return new NodeResult(null, null, null, null, null, Map.of(), true, null, true,
                NodeOutcome.PENDING, Map.of(), null, null, null);
    }

    public static NodeResult suppressed(String suppressedNodeId, String reasonCode, String reasonMessage) {
        return suppressed("suppressed", suppressedNodeId, reasonCode, reasonMessage);
    }

    public static NodeResult suppressed(String routeHandle, String suppressedNodeId, String reasonCode, String reasonMessage) {
        return new NodeResult(null, null, null, null, null, Map.of(), true, null, false,
                NodeOutcome.SUPPRESSED, route(routeHandle, suppressedNodeId), reasonCode, reasonMessage, null);
    }

    public static NodeResult timeout(String timeoutNodeId, String reasonCode, String reasonMessage) {
        return new NodeResult(null, null, null, null, null, Map.of(), true, null, false,
                NodeOutcome.TIMEOUT, route("timeout", timeoutNodeId), reasonCode, reasonMessage, null);
    }

    public static NodeResult skipped(String skippedNodeId, String reasonCode, String reasonMessage) {
        return new NodeResult(null, null, null, null, null, Map.of(), true, null, false,
                NodeOutcome.SKIPPED, route("skipped", skippedNodeId), reasonCode, reasonMessage, null);
    }

    public static NodeResult routed(String routeHandle, String nodeId, Map<String, Object> output) {
        return new NodeResult(null, null, null, null, null, output == null ? Map.of() : output,
                true, null, false, NodeOutcome.SUCCESS, route(routeHandle, nodeId), null, null, null);
    }

    public static NodeResult pending(Long resumeAtEpochMs, String reasonCode, String reasonMessage) {
        return new NodeResult(null, null, null, null, null, Map.of(), true, null, true,
                NodeOutcome.PENDING, Map.of(), reasonCode, reasonMessage, resumeAtEpochMs);
    }

    private static Map<String, String> route(String branch, String nodeId) {
        if (isBlank(nodeId)) {
            return Map.of();
        }
        return Map.of(branch, nodeId);
    }

    private static Map<String, String> routes(Map<String, String> branchMap, String elseNodeId) {
        Map<String, String> routes = new LinkedHashMap<>();
        if (branchMap != null) {
            branchMap.forEach((branch, nodeId) -> {
                if (!isBlank(branch) && !isBlank(nodeId)) {
                    routes.put(branch, nodeId);
                }
            });
        }
        if (!isBlank(elseNodeId)) {
            routes.put("__else", elseNodeId);
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(routes));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
