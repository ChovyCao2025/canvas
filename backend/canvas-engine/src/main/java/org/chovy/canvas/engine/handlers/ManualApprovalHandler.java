package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.approval.CanvasManualApproval;
import org.chovy.canvas.domain.approval.CanvasManualApprovalMapper;
import org.chovy.canvas.domain.constant.ApprovalOnTimeoutAction;
import org.chovy.canvas.domain.constant.ApprovalStatus;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 人工审批节点（设计文档 18.2 节）。
 *
 * 首次进入：创建审批记录、发送通知、标记 ctx WAITING（流程挂起）。
 * 审批后再次触发（通过 /canvas/execution/{id}/approve|reject API）：
 *   - 从 ctx 中读取审批结果，决定走 approveNodeId 还是 rejectNodeId
 * 超时处理：由 Watchdog 检测 timeoutAt 并按 onTimeout 策略处理。
 */
@Component
@Slf4j
@NodeHandlerType("MANUAL_APPROVAL")
@RequiredArgsConstructor
public class ManualApprovalHandler implements NodeHandler {

    private final CanvasManualApprovalMapper approvalMapper;
    private final ObjectMapper objectMapper;

    /** ctx 中审批结果的 key */
    public static final String APPROVAL_RESULT_KEY = "__approval_result_";

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String nodeId       = extractNodeId(config);  // 从 config 获取自身 nodeId（由调用方注入）
        String approveNodeId = (String) config.get("approveNodeId");
        String rejectNodeId  = (String) config.get("rejectNodeId");
        String onTimeout     = (String) config.getOrDefault("onTimeout", ApprovalOnTimeoutAction.REJECT);
        int    timeoutHours  = config.get("timeoutHours") instanceof Number n ? n.intValue() : 24;

        String approvalId = ctx.getExecutionId() + ":" + nodeId;
        String resultKey  = APPROVAL_RESULT_KEY + nodeId;

        // 检查是否已经有审批结果（审批/拒绝 API 写入 ctx 后再次触发）
        Object result = ctx.getContextValue(resultKey);
        if (ApprovalStatus.APPROVED.equals(result)) {
            log.info("[MANUAL_APPROVAL] 审批通过 approvalId={}", approvalId);
            return Mono.just(NodeResult.ok(approveNodeId, Map.of()));
        }
        if (ApprovalStatus.REJECTED.equals(result)) {
            log.info("[MANUAL_APPROVAL] 审批拒绝 approvalId={}", approvalId);
            return approveNodeId != null
                    ? Mono.just(NodeResult.ok(rejectNodeId, Map.of()))
                    : Mono.just(NodeResult.fail("人工审批被拒绝"));
        }

        // 首次进入：创建审批记录，挂起流程
        List<String> approvers = (List<String>) config.getOrDefault("approvers", List.of());
        try {
            CanvasManualApproval approval = CanvasManualApproval.builder()
                    .id(approvalId)
                    .executionId(ctx.getExecutionId())
                    .canvasId(ctx.getCanvasId())
                    .nodeId(nodeId)
                    .userId(ctx.getUserId())
                    .approvers(objectMapper.writeValueAsString(approvers))
                    .onTimeout(onTimeout)
                    .timeoutAt(LocalDateTime.now().plusHours(timeoutHours))
                    .status(ApprovalStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            approvalMapper.insert(approval);
            log.info("[MANUAL_APPROVAL] 创建审批 approvalId={} approvers={} timeout={}h",
                    approvalId, approvers, timeoutHours);
            // TODO: 发送审批通知（钉钉/邮件），Phase 13 接入
        } catch (Exception e) {
            log.error("[MANUAL_APPROVAL] 创建审批记录失败: {}", e.getMessage());
        }

        // 返回 WAITING（设计文档 18.2 节：流程挂起，等待审批）
        // DagEngine 的调用方（CanvasExecutionService）检测 WAITING 状态后持久化 ctx
        ctx.setNodeStatus(nodeId, NodeStatus.WAITING);
        return Mono.just(NodeResult.ok(null, Map.of())); // nextNodeId=null 表示挂起
    }

    private String extractNodeId(Map<String, Object> config) {
        Object id = config.get("__nodeId");
        if (id == null) {
            throw new IllegalStateException(
                "MANUAL_APPROVAL 节点未注入 __nodeId，" +
                "请检查 DagEngine.resolveConfigWithNodeId() 是否被正确调用");
        }
        return id.toString();
    }
}
