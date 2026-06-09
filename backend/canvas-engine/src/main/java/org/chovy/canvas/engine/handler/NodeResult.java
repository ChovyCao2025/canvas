package org.chovy.canvas.engine.handler;

import org.chovy.canvas.common.MapFieldKeys;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 节点执行结果模型。
 * 约定：一个节点可返回“单路下一跳”“多路分支”“终止”“失败”“等待（阈值节点）”等形态。
 * @param nextNodeId 单路下一跳节点 ID.
 * @param successNodeId 成功分支下一跳节点 ID.
 * @param failNodeId 失败分支下一跳节点 ID.
 * @param elseNodeId else 兜底分支下一跳节点 ID.
 * @param branchMap 旧版多分支路由映射，key 为分支标签，value 为节点 ID.
 * @param output 节点输出字段，调度层会合并到执行上下文.
 * @param success 节点是否按业务语义成功.
 * @param errorMessage 节点失败错误信息.
 * @param pending 是否挂起等待更多上游完成或等待恢复调度.
 * @param outcome 节点执行结果归类.
 * @param routes 新版多分支路由映射，key 为出口句柄，value 为节点 ID.
 * @param reasonCode 结果原因编码，用于审计、告警和前端展示.
 * @param reasonMessage 结果原因描述.
 * @param resumeAtEpochMs 挂起恢复时间戳，单位毫秒.
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
        boolean pending,
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

    /** 执行失败，同时保留节点输出字段供审计和前端展示。 */
    public static NodeResult fail(String errorMessage, Map<String, Object> output) {
        return new NodeResult(null, null, null, null, null, output == null ? Map.of() : output,
                false, errorMessage, false, NodeOutcome.FAIL, Map.of(), "NODE_FAILED", errorMessage, null);
    }
    /** 多出口节点结果。 */
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

    /**
     * 执行 suppressed 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param suppressedNodeId suppressedNodeId 对应的业务主键或标识
     * @param reasonCode reasonCode 方法执行所需的业务参数
     * @param reasonMessage reasonMessage 方法执行所需的业务参数
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public static NodeResult suppressed(String suppressedNodeId, String reasonCode, String reasonMessage) {
        return suppressed("suppressed", suppressedNodeId, reasonCode, reasonMessage);
    }

    /**
     * 执行 suppressed 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param routeHandle routeHandle 方法执行所需的业务参数
     * @param suppressedNodeId suppressedNodeId 对应的业务主键或标识
     * @param reasonCode reasonCode 方法执行所需的业务参数
     * @param reasonMessage reasonMessage 方法执行所需的业务参数
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public static NodeResult suppressed(String routeHandle, String suppressedNodeId, String reasonCode, String reasonMessage) {
        return new NodeResult(null, null, null, null, null, Map.of(), true, null, false,
                NodeOutcome.SUPPRESSED, route(routeHandle, suppressedNodeId), reasonCode, reasonMessage, null);
    }

    /**
     * 执行 timeout 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param timeoutNodeId timeoutNodeId 对应的业务主键或标识
     * @param reasonCode reasonCode 方法执行所需的业务参数
     * @param reasonMessage reasonMessage 方法执行所需的业务参数
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public static NodeResult timeout(String timeoutNodeId, String reasonCode, String reasonMessage) {
        return new NodeResult(null, null, null, null, null, Map.of(), true, null, false,
                NodeOutcome.TIMEOUT, route("timeout", timeoutNodeId), reasonCode, reasonMessage, null);
    }

    /**
     * 执行 skipped 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param skippedNodeId skippedNodeId 对应的业务主键或标识
     * @param reasonCode reasonCode 方法执行所需的业务参数
     * @param reasonMessage reasonMessage 方法执行所需的业务参数
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public static NodeResult skipped(String skippedNodeId, String reasonCode, String reasonMessage) {
        return new NodeResult(null, null, null, null, null, Map.of(), true, null, false,
                NodeOutcome.SKIPPED, route("skipped", skippedNodeId), reasonCode, reasonMessage, null);
    }

    /**
     * 执行 routed 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param routeHandle routeHandle 方法执行所需的业务参数
     * @param nodeId nodeId 对应的业务主键或标识
     * @param output output 方法执行所需的业务参数
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public static NodeResult routed(String routeHandle, String nodeId, Map<String, Object> output) {
        return new NodeResult(null, null, null, null, null, output == null ? Map.of() : output,
                true, null, false, NodeOutcome.SUCCESS, route(routeHandle, nodeId), null, null, null);
    }

    /**
     * 执行 pending 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param resumeAtEpochMs resumeAtEpochMs 方法执行所需的业务参数
     * @param reasonCode reasonCode 方法执行所需的业务参数
     * @param reasonMessage reasonMessage 方法执行所需的业务参数
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public static NodeResult pending(Long resumeAtEpochMs, String reasonCode, String reasonMessage) {
        return new NodeResult(null, null, null, null, null, Map.of(), true, null, true,
                NodeOutcome.PENDING, Map.of(), reasonCode, reasonMessage, resumeAtEpochMs);
    }

    /** 返回一个仅替换 output 的结果副本，用于策略类结果保留审计字段。 */
    public NodeResult withOutput(Map<String, Object> nextOutput) {
        return new NodeResult(nextNodeId, successNodeId, failNodeId, elseNodeId, branchMap,
                nextOutput == null ? Map.of() : nextOutput,
                success, errorMessage, pending, outcome, routes, reasonCode, reasonMessage, resumeAtEpochMs);
    }

    /**
     * 执行 route 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param branch branch 方法执行所需的业务参数
     * @param nodeId nodeId 对应的业务主键或标识
     * @return 按业务键组织的映射结果
     */
    private static Map<String, String> route(String branch, String nodeId) {
        if (isBlank(nodeId)) {
            return Map.of();
        }
        return Map.of(branch, nodeId);
    }

    /**
     * 执行 routes 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param branchMap branchMap 方法执行所需的业务参数
     * @param elseNodeId elseNodeId 对应的业务主键或标识
     * @return 按业务键组织的映射结果
     */
    private static Map<String, String> routes(Map<String, String> branchMap, String elseNodeId) {
        Map<String, String> routes = new LinkedHashMap<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (branchMap != null) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            branchMap.forEach((branch, nodeId) -> {
                if (!isBlank(branch) && !isBlank(nodeId)) {
                    routes.put(branch, nodeId);
                }
            });
        }
        if (!isBlank(elseNodeId)) {
            routes.put(MapFieldKeys.ELSE, elseNodeId);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return Collections.unmodifiableMap(new LinkedHashMap<>(routes));
    }

    /**
     * 判断 is Blank 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
