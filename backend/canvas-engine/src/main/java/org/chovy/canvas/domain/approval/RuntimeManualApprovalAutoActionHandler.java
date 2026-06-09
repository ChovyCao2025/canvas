package org.chovy.canvas.domain.approval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.ApprovalOnTimeoutAction;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.ApprovalInstanceDO;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.chovy.canvas.infrastructure.reactor.TrackedReactiveTaskRegistry;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RuntimeManualApprovalAutoActionHandler 编排 domain.approval 场景的领域业务规则。
 */
@Slf4j
@Component
public class RuntimeManualApprovalAutoActionHandler implements ApprovalAutoActionHandler {

    public static final String AUTO_ACTION = "RESUME_RUNTIME_APPROVAL";

    private final CanvasExecutionService executionService;
    private final ObjectMapper objectMapper;
    private final TrackedReactiveTaskRegistry reactiveTaskRegistry;

    /**
     * 创建 RuntimeManualApprovalAutoActionHandler 实例并注入 domain.approval 场景依赖。
     * @param executionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param reactiveTaskRegistry reactive task registry 参数，用于 RuntimeManualApprovalAutoActionHandler 流程中的校验、计算或对象转换。
     */
    public RuntimeManualApprovalAutoActionHandler(CanvasExecutionService executionService,
                                                  ObjectMapper objectMapper,
                                                  TrackedReactiveTaskRegistry reactiveTaskRegistry) {
        this.executionService = executionService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.reactiveTaskRegistry = reactiveTaskRegistry == null
                ? TrackedReactiveTaskRegistry.direct()
                : reactiveTaskRegistry;
    }

    @Override
    /**
     * 判断是否处理运行时人工审批恢复动作。
     */
    public boolean supports(String autoAction) {
        return AUTO_ACTION.equals(autoAction);
    }

    @Override
    /**
     * 判断给定审批状态是否需要触发运行时恢复。
     * 通过、驳回和过期都会回写执行引擎，仍在等待的状态不会触发。
     */
    public boolean supportsStatus(String autoAction, String status) {
        return supports(autoAction)
                && (ApprovalWorkflowService.STATUS_APPROVED.equalsIgnoreCase(status)
                || ApprovalWorkflowService.STATUS_REJECTED.equalsIgnoreCase(status)
                || ApprovalWorkflowService.STATUS_EXPIRED.equalsIgnoreCase(status));
    }

    @Override
    /**
     * 根据审批实例快照恢复被人工审批节点挂起的执行流。
     * 方法会解析 canvasId、userId、nodeId 和 executionId，提交异步触发任务到执行引擎；超时 KEEP_WAITING 时只记录日志不恢复。
     */
    public void execute(ApprovalInstanceDO instance, String actor) {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, Object> snapshot = snapshot(instance.getSnapshotJson());
        Long canvasId = longValue(snapshot.get("canvasId"));
        String userId = string(snapshot.get("userId"));
        String nodeId = string(snapshot.get("nodeId"));
        String executionId = string(snapshot.get("executionId"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (canvasId == null || userId == null || nodeId == null || executionId == null) {
            throw new IllegalStateException("runtime approval snapshot is incomplete: " + instance.getId());
        }

        String effectiveStatus = effectiveResumeStatus(instance.getStatus(), snapshot);
        if (ApprovalOnTimeoutAction.KEEP_WAITING.equals(effectiveStatus)) {
            log.info("[APPROVAL] runtime approval keeps waiting instanceId={}", instance.getId());
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(MapFieldKeys.SOURCE_NODE_ID, nodeId);
        payload.put(MapFieldKeys.WAIT_RESUME_STATUS, effectiveStatus);
        payload.put(MapFieldKeys.APPROVAL_STATUS, effectiveStatus);
        payload.put(MapFieldKeys.APPROVAL_INSTANCE_ID, instance.getId());
        payload.put("approvalActor", actor);
        payload.put("approvalCompletedAt", instance.getCompletedAt() == null ? null : instance.getCompletedAt().toString());
        String triggerType = ApprovalWorkflowService.STATUS_EXPIRED.equalsIgnoreCase(instance.getStatus())
                ? TriggerType.APPROVAL_TIMEOUT
                : TriggerType.APPROVAL_RESUME;
        String msgId = executionId + ":approval:" + instance.getId() + ":" + effectiveStatus;

        reactiveTaskRegistry.submit(
                "runtime-approval-resume-" + instance.getId(),
                executionService.trigger(canvasId, userId, triggerType, NodeType.MANUAL_APPROVAL,
                                nodeId, payload, msgId, false)
                        .doOnNext(ignored -> log.info("[APPROVAL] runtime approval resumed instanceId={} status={}",
                                instance.getId(), effectiveStatus))
                        .then(),
                error -> log.error("[APPROVAL] runtime approval resume failed instanceId={}: {}",
                        instance.getId(), error.getMessage()));
    }

    /**
     * 执行 effectiveResumeStatus 流程，围绕 effective resume status 完成校验、计算或结果组装。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param String string 参数，用于 effectiveResumeStatus 流程中的校验、计算或对象转换。
     * @param snapshot snapshot 参数，用于 effectiveResumeStatus 流程中的校验、计算或对象转换。
     * @return 返回 effective resume status 生成的文本或业务键。
     */
    private String effectiveResumeStatus(String status, Map<String, Object> snapshot) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (ApprovalWorkflowService.STATUS_EXPIRED.equalsIgnoreCase(status)) {
            String onTimeout = string(snapshot.get("onTimeout"));
            if (ApprovalOnTimeoutAction.APPROVE.equalsIgnoreCase(onTimeout)) {
                return ApprovalWorkflowService.STATUS_APPROVED;
            }
            if (ApprovalOnTimeoutAction.KEEP_WAITING.equalsIgnoreCase(onTimeout)) {
                return ApprovalOnTimeoutAction.KEEP_WAITING;
            }
            return ApprovalWorkflowService.STATUS_REJECTED;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return status;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param raw raw 参数，用于 snapshot 流程中的校验、计算或对象转换。
     * @return 返回 snapshot 流程生成的业务结果。
     */
    private Map<String, Object> snapshot(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {});
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalStateException("runtime approval snapshot parse failed", e);
        }
    }

    /**
     * 执行 longValue 流程，围绕 long value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 long value 计算得到的数量、金额或指标值。
     */
    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }

    /**
     * 执行 string 流程，围绕 string 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 string 生成的文本或业务键。
     */
    private String string(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return String.valueOf(value).trim();
    }
}
