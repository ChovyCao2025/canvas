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

@Slf4j
@Component
public class RuntimeManualApprovalAutoActionHandler implements ApprovalAutoActionHandler {

    public static final String AUTO_ACTION = "RESUME_RUNTIME_APPROVAL";

    private final CanvasExecutionService executionService;
    private final ObjectMapper objectMapper;
    private final TrackedReactiveTaskRegistry reactiveTaskRegistry;

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
    public boolean supports(String autoAction) {
        return AUTO_ACTION.equals(autoAction);
    }

    @Override
    public boolean supportsStatus(String autoAction, String status) {
        return supports(autoAction)
                && (ApprovalWorkflowService.STATUS_APPROVED.equalsIgnoreCase(status)
                || ApprovalWorkflowService.STATUS_REJECTED.equalsIgnoreCase(status)
                || ApprovalWorkflowService.STATUS_EXPIRED.equalsIgnoreCase(status));
    }

    @Override
    public void execute(ApprovalInstanceDO instance, String actor) {
        Map<String, Object> snapshot = snapshot(instance.getSnapshotJson());
        Long canvasId = longValue(snapshot.get("canvasId"));
        String userId = string(snapshot.get("userId"));
        String nodeId = string(snapshot.get("nodeId"));
        String executionId = string(snapshot.get("executionId"));
        if (canvasId == null || userId == null || nodeId == null || executionId == null) {
            throw new IllegalStateException("runtime approval snapshot is incomplete: " + instance.getId());
        }

        String effectiveStatus = effectiveResumeStatus(instance.getStatus(), snapshot);
        if (ApprovalOnTimeoutAction.KEEP_WAITING.equals(effectiveStatus)) {
            log.info("[APPROVAL] runtime approval keeps waiting instanceId={}", instance.getId());
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

    private String effectiveResumeStatus(String status, Map<String, Object> snapshot) {
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
        return status;
    }

    private Map<String, Object> snapshot(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("runtime approval snapshot parse failed", e);
        }
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }

    private String string(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return String.valueOf(value).trim();
    }
}
