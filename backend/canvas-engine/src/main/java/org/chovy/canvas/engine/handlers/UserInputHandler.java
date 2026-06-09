package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.domain.canvas.UserInputService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeOutcome;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * UserInputHandler 参与 engine.handlers 场景的画布执行引擎处理。
 */
@Component
@NodeHandlerType(NodeType.USER_INPUT)
public class UserInputHandler implements NodeHandler {

    private static final String FORM_SCHEMA = "formSchema";
    private static final String INPUT_STATUS = "inputStatus";
    private static final String INPUT_RESPONSE_ID = "inputResponseId";
    private static final String INPUT_RESPONSE = "inputResponse";

    private final UserInputService service;

    /**
     * 创建 UserInputHandler 实例并注入 engine.handlers 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     */
    public UserInputHandler(UserInputService service) {
        this.service = service;
    }

    /**
     * 执行用户输入节点：首次进入创建待填写表单并挂起执行，恢复进入时按完成或超时状态继续路由。
     *
     * <p>首次进入会调用 {@link UserInputService#createPending(ExecutionContext, String, Object, String, String, LocalDateTime)}
     * 持久化待输入记录，这是本节点的主要外部副作用；方法本身不声明事务。完成恢复会把用户提交内容写入
     * 输出并走 completedNodeId 或 nextNodeId，超时恢复会输出过期状态并按 timeoutNodeId 路由。</p>
     *
     * @param config 节点配置，关键字段包括表单 schema、完成节点、超时节点和最大等待时长
     * @param ctx 执行上下文，提供用户、执行标识和恢复触发载荷
     * @return 节点结果；首次进入返回 pending，恢复进入返回完成或超时结果
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // 准备本次处理所需的上下文和中间变量。
        String resumeStatus = resumeStatus(config, ctx);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (UserInputService.STATUS_COMPLETED.equalsIgnoreCase(resumeStatus)) {
            return Mono.just(completed(config, ctx));
        }
        if (UserInputService.STATUS_EXPIRED.equalsIgnoreCase(resumeStatus)
                || MapFieldKeys.TIMEOUT.equalsIgnoreCase(resumeStatus)) {
            return Mono.just(timeout(config, ctx));
        }

        Object schema = config.get(FORM_SCHEMA);
        if (schema == null || (schema instanceof String text && text.isBlank())) {
            return Mono.just(NodeResult.fail("USER_INPUT: formSchema is required"));
        }
        String nodeId = string(config.get(MapFieldKeys.NODE_ID_INTERNAL), null);
        String completedNodeId = string(config.get("completedNodeId"), string(config.get(MapFieldKeys.NEXT_NODE_ID), null));
        String timeoutNodeId = string(config.get(MapFieldKeys.TIMEOUT_NODE_ID), null);
        LocalDateTime expiresAt = expiresAt(config);
        UserInputService.PendingInput pending = service.createPending(ctx, nodeId, schema,
                completedNodeId, timeoutNodeId, expiresAt);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(INPUT_STATUS, pending.status());
        output.put(INPUT_RESPONSE_ID, pending.responseId());
        output.put(MapFieldKeys.TIMEOUT_NODE_ID, pending.timeoutNodeId());
        Long resumeAt = pending.expiresAt() == null
                ? null
                : pending.expiresAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return Mono.just(new NodeResult(null, null, null, null, null, output, true, null,
                true, NodeOutcome.PENDING, Map.of(), "USER_INPUT_PENDING",
                "waiting for user input", resumeAt));
    }

    /**
     * 构造用户已提交后的成功结果，并把响应 ID 与提交内容写回上下文。
     */
    private NodeResult completed(Map<String, Object> config, ExecutionContext ctx) {
        Map<String, Object> output = new LinkedHashMap<>();
        Map<String, Object> payload = ctx.getTriggerPayload();
        output.put(INPUT_STATUS, UserInputService.STATUS_COMPLETED);
        output.put(INPUT_RESPONSE_ID, payload.get(INPUT_RESPONSE_ID));
        output.put(INPUT_RESPONSE, payload.getOrDefault(INPUT_RESPONSE, Map.of()));
        String next = string(payload.get("completedNodeId"),
                string(config.get("completedNodeId"), string(config.get(MapFieldKeys.NEXT_NODE_ID), null)));
        return NodeResult.ok(next, output);
    }

    /**
     * 构造用户输入超时结果，保留响应 ID 并在配置了 timeoutNodeId 时走超时分支。
     */
    private NodeResult timeout(Map<String, Object> config, ExecutionContext ctx) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(INPUT_STATUS, UserInputService.STATUS_EXPIRED);
        output.put(INPUT_RESPONSE_ID, ctx.getTriggerPayload().get(INPUT_RESPONSE_ID));
        String timeoutNodeId = string(ctx.getTriggerPayload().get(MapFieldKeys.TIMEOUT_NODE_ID),
                string(config.get(MapFieldKeys.TIMEOUT_NODE_ID), null));
        return new NodeResult(null, null, null, null, null, output, true, null, false,
                NodeOutcome.TIMEOUT,
                timeoutNodeId == null ? Map.of() : Map.of("timeout", timeoutNodeId),
                "USER_INPUT_TIMEOUT",
                "user input timed out",
                null);
    }

    /**
     * 从节点配置或恢复触发载荷读取用户输入状态，配置值优先用于测试和调度恢复。
     */
    private String resumeStatus(Map<String, Object> config, ExecutionContext ctx) {
        String value = string(config.get(MapFieldKeys.WAIT_RESUME_STATUS), null);
        if (value != null) {
            return value;
        }
        return string(ctx.getTriggerPayload().get(MapFieldKeys.WAIT_RESUME_STATUS), null);
    }

    /**
     * 根据 maxWait 计算待输入记录的过期时间；未配置时表示不自动超时。
     */
    private LocalDateTime expiresAt(Map<String, Object> config) {
        Object raw = config.get(MapFieldKeys.MAX_WAIT);
        Duration duration = duration(raw);
        return duration == null ? null : LocalDateTime.now().plus(duration);
    }

    @SuppressWarnings("unchecked")
    /**
     * 解析用户输入节点的等待时长配置，兼容 value/unit 与 durationValue/durationUnit。
     */
    private Duration duration(Object raw) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        Object value = ((Map<Object, Object>) map).getOrDefault("value", map.get("durationValue"));
        Object unit = ((Map<Object, Object>) map).getOrDefault("unit", map.get("durationUnit"));
        Number number;
        if (value instanceof Number parsed) {
            number = parsed;
        } else {
            if (value == null || String.valueOf(value).isBlank()) {
                return null;
            }
            number = Double.valueOf(String.valueOf(value));
        }
        long amount = number.longValue();
        String normalizedUnit = unit == null ? "MINUTES" : String.valueOf(unit).toUpperCase();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return switch (normalizedUnit) {
            case "SECOND", "SECONDS" -> Duration.ofSeconds(amount);
            case "HOUR", "HOURS" -> Duration.ofHours(amount);
            case "DAY", "DAYS" -> Duration.ofDays(amount);
            default -> Duration.ofMinutes(amount);
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
