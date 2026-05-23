package org.chovy.canvas.engine.handler;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
    public static NodeResult ok(String nextNodeId, Map<String, Object> output) {
        return new NodeResult(nextNodeId, null, null, null, null, output, true, null, false,
                NodeOutcome.SUCCESS, route("success", nextNodeId), null, null, null);
    }
    public static NodeResult terminal(Map<String, Object> output) {
        return new NodeResult(null, null, null, null, null, output, true, null, false,
                NodeOutcome.SUCCESS, Map.of(), null, null, null);
    }
    public static NodeResult ifResult(boolean condition, String successId, String failId) {
        return new NodeResult(null, condition ? successId : null, !condition ? failId : null,
                null, null, Map.of(), true, null, false,
                NodeOutcome.SUCCESS, route(condition ? "success" : "fail", condition ? successId : failId),
                null, null, null);
    }
    public static NodeResult fail(String errorMessage) {
        return new NodeResult(null, null, null, null, null, Map.of(), false, errorMessage, false,
                NodeOutcome.FAIL, Map.of(), "NODE_FAILED", errorMessage, null);
    }
    public static NodeResult multiNext(Map<String, String> branchMap, String elseNodeId) {
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
        return new NodeResult(null, null, null, null, null, Map.of(), true, null, false,
                NodeOutcome.SUPPRESSED, route("suppressed", suppressedNodeId), reasonCode, reasonMessage, null);
    }

    public static NodeResult timeout(String timeoutNodeId, String reasonCode, String reasonMessage) {
        return new NodeResult(null, null, null, null, null, Map.of(), true, null, false,
                NodeOutcome.TIMEOUT, route("timeout", timeoutNodeId), reasonCode, reasonMessage, null);
    }

    public static NodeResult skipped(String skippedNodeId, String reasonCode, String reasonMessage) {
        return new NodeResult(null, null, null, null, null, Map.of(), true, null, false,
                NodeOutcome.SKIPPED, route("skipped", skippedNodeId), reasonCode, reasonMessage, null);
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
