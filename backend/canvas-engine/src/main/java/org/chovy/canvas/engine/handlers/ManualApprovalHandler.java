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

/**
 * ManualApprovalHandler 参与 engine.handlers 场景的画布执行引擎处理。
 */
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

    /**
     * 创建 ManualApprovalHandler 实例并注入 engine.handlers 场景依赖。
     * @param workflowService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public ManualApprovalHandler(ApprovalWorkflowService workflowService, ObjectMapper objectMapper) {
        this.workflowService = workflowService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 执行人工审批节点：首次进入时创建运行时审批单并挂起执行，恢复进入时按审批状态选择后续路由。
     *
     * <p>首次进入会调用 {@link ApprovalWorkflowService#submit(ApprovalSubmitCommand)} 写入审批实例，
     * 这是本节点的主要外部副作用；方法本身不声明事务，审批服务负责持久化边界。恢复时从节点配置或
     * 触发载荷读取 {@code waitResumeStatus}：通过走批准分支，拒绝走拒绝分支，超时按
     * {@code onTimeout} 决定自动批准、继续等待或拒绝。</p>
     *
     * @param config 节点配置，包含审批人、审批原因、超时策略以及批准/拒绝/默认下一跳
     * @param ctx 执行上下文，提供租户、执行、版本、用户和恢复触发载荷
     * @return 节点结果；首次进入返回 pending 并输出审批实例 ID，恢复进入返回批准、拒绝或继续等待结果
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // 准备本次处理所需的上下文和中间变量。
        String resumeStatus = resumeStatus(config, ctx);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return Mono.just(NodeResult.pending(null,
                "MANUAL_APPROVAL_PENDING",
                "waiting for runtime manual approval")
                .withOutput(Map.of(MapFieldKeys.APPROVAL_INSTANCE_ID, approval.id())));
    }

    /**
     * 构造审批通过后的成功结果，并优先使用 approveNodeId、successNodeId、nextNodeId 作为下一跳。
     */
    private NodeResult approved(Map<String, Object> config, ExecutionContext ctx) {
        Map<String, Object> output = output(ctx, ApprovalWorkflowService.STATUS_APPROVED);
        String nextNodeId = string(config.get(APPROVE_NODE_ID),
                string(config.get(MapFieldKeys.SUCCESS_NODE_ID), string(config.get(MapFieldKeys.NEXT_NODE_ID), null)));
        return NodeResult.ok(nextNodeId, output);
    }

    /**
     * 构造审批拒绝后的结果；配置拒绝节点时路由到该节点，否则以当前输出结束执行。
     */
    private NodeResult rejected(Map<String, Object> config, ExecutionContext ctx) {
        Map<String, Object> output = output(ctx, ApprovalWorkflowService.STATUS_REJECTED);
        String rejectNodeId = string(config.get(REJECT_NODE_ID), string(config.get(MapFieldKeys.FAIL_NODE_ID), null));
        if (rejectNodeId == null) {
            return NodeResult.terminal(output);
        }
        return NodeResult.routed("reject", rejectNodeId, output);
    }

    /**
     * 处理审批超时恢复，按 onTimeout 把超时转换为自动通过、继续挂起或拒绝。
     */
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

    /**
     * 生成审批节点写回上下文的状态输出，并在恢复载荷存在时保留审批实例 ID。
     */
    private Map<String, Object> output(ExecutionContext ctx, String status) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(MapFieldKeys.APPROVAL_STATUS, status);
        Object approvalInstanceId = ctx.getTriggerPayload().get(MapFieldKeys.APPROVAL_INSTANCE_ID);
        if (approvalInstanceId != null) {
            output.put(MapFieldKeys.APPROVAL_INSTANCE_ID, approvalInstanceId);
        }
        return output;
    }

    /**
     * 解析审批恢复状态，配置中显式注入的状态优先于触发载荷中的状态。
     */
    private String resumeStatus(Map<String, Object> config, ExecutionContext ctx) {
        String value = string(config.get(MapFieldKeys.WAIT_RESUME_STATUS), null);
        if (value != null) {
            return value;
        }
        return string(ctx.getTriggerPayload().get(MapFieldKeys.WAIT_RESUME_STATUS), null);
    }

    /**
     * 序列化审批单的恢复快照，供审批系统完成后重新定位执行、节点和后续路由。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("manual approval snapshot serialization failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    /**
     * 规范化审批人配置，兼容数组和逗号分隔字符串两种输入形式。
     */
    private List<String> approvers(Object raw) {
        List<String> values = new ArrayList<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (raw instanceof Iterable<?> iterable) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return values;
    }

    /**
     * 将节点的 timeoutHours 或 maxWait 配置转换为审批系统使用的小时级截止时间。
     */
    private Integer dueHours(Map<String, Object> config) {
        // 准备本次处理所需的上下文和中间变量。
        Object timeoutHours = config.get("timeoutHours");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 解析 maxWait 的数值和单位，供审批截止时间换算使用。
     */
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

    /**
     * 将对象转换为非空字符串。
     *
     * @param value 原始值
     * @param fallback 默认值
     * @return 字符串值或默认值
     */
    private String string(Object value, String fallback) {
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return String.valueOf(value).trim();
    }
}
