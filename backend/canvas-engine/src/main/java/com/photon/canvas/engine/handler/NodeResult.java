package com.photon.canvas.engine.handler;

import com.photon.canvas.engine.context.ExecutionContext;
import java.util.Map;

/**
 * 节点执行结果
 */
public record NodeResult(
        String nextNodeId,            // 普通节点的下一个节点
        String successNodeId,         // IF_CONDITION 成功分支
        String failNodeId,            // IF_CONDITION 失败分支
        String elseNodeId,            // SELECTOR 否则分支
        Map<String, String> branchMap, // SELECTOR/AB_SPLIT/PRIORITY 多出边 {handleId → nodeId}
        Map<String, Object> output,   // 写入上下文的输出字段
        boolean success,
        String errorMessage
) {
    /** 普通成功结果 */
    public static NodeResult ok(String nextNodeId, Map<String, Object> output) {
        return new NodeResult(nextNodeId, null, null, null, null, output, true, null);
    }

    /** 成功但无后续节点（终止节点） */
    public static NodeResult terminal(Map<String, Object> output) {
        return new NodeResult(null, null, null, null, null, output, true, null);
    }

    /** IF_CONDITION 结果 */
    public static NodeResult ifResult(boolean condition, String successId, String failId) {
        return new NodeResult(null, condition ? successId : null, !condition ? failId : null,
                null, null, Map.of(), true, null);
    }

    /** 失败结果 */
    public static NodeResult fail(String errorMessage) {
        return new NodeResult(null, null, null, null, null, Map.of(), false, errorMessage);
    }

    /** 多分支结果（SELECTOR/AB_SPLIT/PRIORITY） */
    public static NodeResult multiNext(Map<String, String> branchMap, String elseNodeId) {
        return new NodeResult(null, null, null, elseNodeId, branchMap, Map.of(), true, null);
    }
}
