package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.ApprovalOnTimeoutAction;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.domain.approval.ApprovalInstanceView;
import org.chovy.canvas.domain.approval.ApprovalSubmitCommand;
import org.chovy.canvas.domain.approval.ApprovalWorkflowService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeOutcome;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@NodeHandlerType(NodeType.MANUAL_APPROVAL)
public class ManualApprovalHandler implements NodeHandler {

    static final String DEFINITION_KEY = "RUNTIME_MANUAL_DEFAULT";
    static final String DOMAIN = "RUNTIME";
    static final String TARGET_TYPE = "EXECUTION_NODE";
    static final String AUTO_ACTION = "RESUME_RUNTIME_APPROVAL";

    private static final String APPROVERS = "approvers";
    private static final String ON_TIMEOUT = "onTimeout";
    private static final String APPROVE_NODE_ID = "approveNodeId";
    private static final String REJECT_NODE_ID = "rejectNodeId";
    private static final String APPROVAL_REASON = "approvalReason";

    private final ApprovalWorkflowService workflowService;
    private final ObjectMapper objectMapper;

    public ManualApprovalHandler(ApprovalWorkflowService workflowService, ObjectMapper objectMapper) {
        this.workflowService = workflowService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String resumeStatus = resumeStatus(config, ctx);
        if (ApprovalWorkflowService.STATUS_APPROVED.equalsIgnoreCase(resumeStatus)) {
            return Mono.just(approved(config, ctx));
        }
        if (ApprovalWorkflowService.STATUS_REJECTED.equalsIgnoreCase(resumeStatus)) {
            return Mono.just(rejected(config, ctx));
        }
        if (ApprovalWorkflowService.STATUS_EXPIRED.equalsIgnoreCase(resumeStatus)
                || MapFieldKeys.TIMEOUT.equalsIgnoreCase(resumeStatus)) {
            return Mono.just(timedOut(config, ctx));
        }

        String nodeId = string(config.get(MapFieldKeys.NODE_ID_INTERNAL), null);
        if (nodeId == null) {
            return Mono.just(NodeResult.fail("MANUAL_APPROVAL node id is required"));
        }
        ApprovalInstanceView approval = workflowService.submit(new ApprovalSubmitCommand(
                ctx.getTenantId(),
                DEFINITION_KEY,
                DOMAIN,
                TARGET_TYPE,
                ctx.getExecutionId(),
                ctx.getVersionId(),
                ctx.getUserId(),
                string(config.get(APPROVAL_REASON), "runtime manual approval"),
                "HIGH",
                "[\"RUNTIME_MANUAL_APPROVAL\"]",
                snapshot(config, ctx, nodeId),
                approvers(config.get(APPROVERS)),
                dueHours(config),
                AUTO_ACTION));
        return Mono.just(NodeResult.pending(null,
                "MANUAL_APPROVAL_PENDING",
                "waiting for runtime manual approval")
                .withOutput(Map.of(MapFieldKeys.APPROVAL_INSTANCE_ID, approval.id())));
    }

    private NodeResult approved(Map<String, Object> config, ExecutionContext ctx) {
        Map<String, Object> output = output(ctx, ApprovalWorkflowService.STATUS_APPROVED);
        String nextNodeId = string(config.get(APPROVE_NODE_ID),
                string(config.get(MapFieldKeys.SUCCESS_NODE_ID), string(config.get(MapFieldKeys.NEXT_NODE_ID), null)));
        return NodeResult.ok(nextNodeId, output);
    }

    private NodeResult rejected(Map<String, Object> config, ExecutionContext ctx) {
        Map<String, Object> output = output(ctx, ApprovalWorkflowService.STATUS_REJECTED);
        String rejectNodeId = string(config.get(REJECT_NODE_ID), string(config.get(MapFieldKeys.FAIL_NODE_ID), null));
        if (rejectNodeId == null) {
            return NodeResult.terminal(output);
        }
        return NodeResult.routed("reject", rejectNodeId, output);
    }

    private NodeResult timedOut(Map<String, Object> config, ExecutionContext ctx) {
        String onTimeout = string(config.get(ON_TIMEOUT), ApprovalOnTimeoutAction.REJECT).toUpperCase(Locale.ROOT);
        if (ApprovalOnTimeoutAction.APPROVE.equals(onTimeout)) {
            return approved(config, ctx);
        }
        if (ApprovalOnTimeoutAction.KEEP_WAITING.equals(onTimeout)) {
            return NodeResult.pending(null, "MANUAL_APPROVAL_KEEP_WAITING", "manual approval kept waiting");
        }
        return rejected(config, ctx);
    }

    private Map<String, Object> output(ExecutionContext ctx, String status) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(MapFieldKeys.APPROVAL_STATUS, status);
        Object approvalInstanceId = ctx.getTriggerPayload().get(MapFieldKeys.APPROVAL_INSTANCE_ID);
        if (approvalInstanceId != null) {
            output.put(MapFieldKeys.APPROVAL_INSTANCE_ID, approvalInstanceId);
        }
        return output;
    }

    private String resumeStatus(Map<String, Object> config, ExecutionContext ctx) {
        String value = string(config.get(MapFieldKeys.WAIT_RESUME_STATUS), null);
        if (value != null) {
            return value;
        }
        return string(ctx.getTriggerPayload().get(MapFieldKeys.WAIT_RESUME_STATUS), null);
    }

    private String snapshot(Map<String, Object> config, ExecutionContext ctx, String nodeId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("executionId", ctx.getExecutionId());
        snapshot.put("canvasId", ctx.getCanvasId());
        snapshot.put("versionId", ctx.getVersionId());
        snapshot.put("userId", ctx.getUserId());
        snapshot.put("nodeId", nodeId);
        snapshot.put("approveNodeId", string(config.get(APPROVE_NODE_ID), string(config.get(MapFieldKeys.NEXT_NODE_ID), null)));
        snapshot.put("rejectNodeId", string(config.get(REJECT_NODE_ID), string(config.get(MapFieldKeys.FAIL_NODE_ID), null)));
        snapshot.put("onTimeout", string(config.get(ON_TIMEOUT), ApprovalOnTimeoutAction.REJECT));
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new IllegalArgumentException("manual approval snapshot serialization failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> approvers(Object raw) {
        List<String> values = new ArrayList<>();
        if (raw instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                String value = string(item, null);
                if (value != null) {
                    values.add(value);
                }
            }
        } else {
            String value = string(raw, null);
            if (value != null) {
                for (String item : value.split(",")) {
                    String approver = string(item, null);
                    if (approver != null) {
                        values.add(approver);
                    }
                }
            }
        }
        return values;
    }

    private Integer dueHours(Map<String, Object> config) {
        Object timeoutHours = config.get("timeoutHours");
        if (timeoutHours instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        Object maxWait = config.get(MapFieldKeys.MAX_WAIT);
        if (maxWait instanceof Map<?, ?> map) {
            Duration duration = duration(map.get("value"), map.get("unit"));
            if (duration != null) {
                return Math.max(1, (int) Math.ceil(duration.toMinutes() / 60.0d));
            }
        }
        return null;
    }

    private Duration duration(Object rawValue, Object rawUnit) {
        if (rawValue == null || String.valueOf(rawValue).isBlank()) {
            return null;
        }
        long value = rawValue instanceof Number number
                ? number.longValue()
                : Long.parseLong(String.valueOf(rawValue));
        String unit = rawUnit == null ? "HOURS" : String.valueOf(rawUnit).toUpperCase(Locale.ROOT);
        return switch (unit) {
            case "SECOND", "SECONDS" -> Duration.ofSeconds(value);
            case "MINUTE", "MINUTES" -> Duration.ofMinutes(value);
            case "DAY", "DAYS" -> Duration.ofDays(value);
            default -> Duration.ofHours(value);
        };
    }

    private String string(Object value, String fallback) {
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return String.valueOf(value).trim();
    }
}
