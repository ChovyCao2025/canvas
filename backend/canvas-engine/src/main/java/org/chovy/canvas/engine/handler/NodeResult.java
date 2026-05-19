package org.chovy.canvas.engine.handler;

import org.chovy.canvas.engine.context.ExecutionContext;
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
        boolean pending               // true=阈值未满足，挂起等待更多上游完成（THRESHOLD 节点专用）
) {
    public static NodeResult ok(String nextNodeId, Map<String, Object> output) {
        return new NodeResult(nextNodeId, null, null, null, null, output, true, null, false);
    }
    public static NodeResult terminal(Map<String, Object> output) {
        return new NodeResult(null, null, null, null, null, output, true, null, false);
    }
    public static NodeResult ifResult(boolean condition, String successId, String failId) {
        return new NodeResult(null, condition ? successId : null, !condition ? failId : null,
                null, null, Map.of(), true, null, false);
    }
    public static NodeResult fail(String errorMessage) {
        return new NodeResult(null, null, null, null, null, Map.of(), false, errorMessage, false);
    }
    public static NodeResult multiNext(Map<String, String> branchMap, String elseNodeId) {
        return new NodeResult(null, null, null, elseNodeId, branchMap, Map.of(), true, null, false);
    }

    /**
     * 阈值未满足，挂起等待更多上游完成（THRESHOLD 节点专用）。
     * 以 {@code success=true, pending=true} 实现，使其通过 {@code executeHandlerWithRepeat}
     * 的 repeat 检查路径——持锁期间到来的上游信号通过 repeatPending 被捕获，
     * repeat 重新评估时若阈值满足则正确路由。
     */
    public static NodeResult waiting() {
        return new NodeResult(null, null, null, null, null, Map.of(), true, null, true);
    }
}
