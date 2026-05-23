package org.chovy.canvas.engine.handler;

import org.chovy.canvas.engine.context.ExecutionContext;
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
        boolean pending               // true=阈值未满足，挂起等待更多上游完成（THRESHOLD 节点专用）
) {
    // 说明：
    // - nextNodeId 与 success/fail/branchMap 互斥使用；
    // - output 会合并到 ExecutionContext.flatContext（由调度层处理）。

    /** 普通单路成功（nextNodeId）。 */
    public static NodeResult ok(String nextNodeId, Map<String, Object> output) {
        return new NodeResult(nextNodeId, null, null, null, null, output, true, null, false);
    }
    /** 正常终止（无下一跳）。 */
    public static NodeResult terminal(Map<String, Object> output) {
        return new NodeResult(null, null, null, null, null, output, true, null, false);
    }
    /** IF 类节点的二路分支结果。 */
    public static NodeResult ifResult(boolean condition, String successId, String failId) {
        return new NodeResult(null, condition ? successId : null, !condition ? failId : null,
                null, null, Map.of(), true, null, false);
    }
    /** 执行失败。 */
    public static NodeResult fail(String errorMessage) {
        return new NodeResult(null, null, null, null, null, Map.of(), false, errorMessage, false);
    }
    /** 多分支节点（如 PRIORITY / AB_SPLIT / SELECTOR）。 */
    public static NodeResult multiNext(Map<String, String> branchMap, String elseNodeId) {
        // branchMap key 仅作为分支标签，value 才是实际节点 ID
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
