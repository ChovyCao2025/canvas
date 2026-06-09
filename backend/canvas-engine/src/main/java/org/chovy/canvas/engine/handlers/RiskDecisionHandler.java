package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionAction;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionRequest;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionResponse;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 画布风控决策节点处理器，将执行上下文映射为风控请求，并按决策动作选择后续路由。
 */
@Component
@NodeHandlerType(NodeType.RISK_DECISION)
public class RiskDecisionHandler implements NodeHandler {

    private static final String DEFAULT_FAIL_POLICY = "FAIL_REVIEW";

    private final RiskDecisionService decisionService;

    /**
     * 创建风控节点处理器。
     */
    public RiskDecisionHandler(RiskDecisionService decisionService) {
        this.decisionService = decisionService;
    }

    /**
     * 在线程池中异步执行风控决策，避免阻塞画布执行主流程。
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        return Mono.fromCallable(() -> execute(config == null ? Map.of() : config, ctx))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 读取节点配置和执行上下文，组装运行时风控请求并返回带路由的节点结果。
     */
    private NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
        MappingResult subject = mappedValues(config.get("subjectMapping"), ctx, false);
        if (subject.missingRequired()) {
            // 主体标识是幂等去重、金丝雀分桶和名单匹配的必要输入。
            return failPolicyResult(config, "MISSING_REQUIRED_MAPPING");
        }
        Map<String, Object> event = mappedValues(config.get("eventMapping"), ctx, true).values();
        Map<String, Object> context = mappedValues(config.get("contextMapping"), ctx, true).values();
        // 节点元数据写入 context，而不是主体或事件事实，便于规则作者区分调用来源。
        context.putIfAbsent("caller", "CANVAS_NODE");
        context.put("canvasId", ctx.getCanvasId());
        context.put("nodeId", nodeId(config));
        context.put("executionId", ctx.getExecutionId());

        RiskDecisionResponse response = decisionService.evaluate(new RiskDecisionRequest(
                ctx.getTenantId(),
                requestId(config, ctx),
                string(config.get("sceneKey")),
                Instant.now(),
                event,
                subject.values(),
                context,
                Map.of(),
                intValue(config.get("timeoutMs"), 50)));
        return routedResult(config, response);
    }

    /**
     * 当必要映射缺失时按节点失败策略生成兜底路由结果。
     */
    private NodeResult failPolicyResult(Map<String, Object> config, String reason) {
        RiskDecisionAction action = switch (string(config.getOrDefault("failPolicy", DEFAULT_FAIL_POLICY))) {
            case "FAIL_CLOSED" -> RiskDecisionAction.BLOCK;
            case "FAIL_OPEN" -> RiskDecisionAction.ALLOW;
            default -> RiskDecisionAction.REVIEW;
        };
        String routeAction = routeAction(action);
        return NodeResult.routed(routeAction, route(config, routeAction), Map.of(
                "decision", action.name(),
                "reason", reason));
    }

    /**
     * 将风控服务响应转换为画布节点路由结果。
     */
    private NodeResult routedResult(Map<String, Object> config, RiskDecisionResponse response) {
        String action = routeAction(response.action());
        return NodeResult.routed(action, route(config, action), output(response));
    }

    /**
     * 把风控动作转换为画布路由动作，影子决策保持放行路由。
     */
    private String routeAction(RiskDecisionAction action) {
        // 影子决策只暴露建议，不改变旅程执行分支。
        return action == RiskDecisionAction.SHADOW_ONLY ? RiskDecisionAction.ALLOW.name() : action.name();
    }

    /**
     * 从节点配置的动作路由表中读取目标节点编号。
     */
    @SuppressWarnings("unchecked")
    private String route(Map<String, Object> config, String action) {
        Object routes = config.get("actionRoutes");
        if (routes instanceof Map<?, ?> map) {
            Object nodeId = map.get(action);
            return nodeId == null ? null : nodeId.toString();
        }
        return null;
    }

    /**
     * 构造画布下游可消费的输出数据，同时保留完整风控决策明细。
     */
    private Map<String, Object> output(RiskDecisionResponse response) {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, Object> riskDecision = new LinkedHashMap<>();
        riskDecision.put("requestId", response.requestId());
        riskDecision.put("decisionRunId", response.decisionRunId());
        riskDecision.put("sceneKey", response.sceneKey());
        riskDecision.put("strategyKey", response.strategyKey());
        riskDecision.put("strategyVersion", response.strategyVersion());
        riskDecision.put("decision", response.action().name());
        riskDecision.put("score", response.score());
        riskDecision.put("riskBand", response.riskBand().name());
        riskDecision.put("reasons", response.reasons());
        riskDecision.put("matchedRules", response.matchedRules());
        riskDecision.put("labels", response.labels());
        riskDecision.put("missingFeatures", response.missingFeatures());
        riskDecision.put("traceAvailable", response.traceAvailable());
        riskDecision.put("latencyMs", response.latencyMs());

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("decision", response.action().name());
        if (response.action() == RiskDecisionAction.SHADOW_ONLY) {
            output.put("suggestedDecision", RiskDecisionAction.SHADOW_ONLY.name());
        }
        output.put("riskDecision", riskDecision);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return output;
    }

    /**
     * 按配置映射从执行上下文中提取字段，必要映射缺失时标记失败。
     */
    @SuppressWarnings("unchecked")
    private MappingResult mappedValues(Object mappingConfig, ExecutionContext ctx, boolean allowConstants) {
        if (!(mappingConfig instanceof Map<?, ?> mappings) || mappings.isEmpty()) {
            return new MappingResult(Map.of(), true);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : mappings.entrySet()) {
            String key = entry.getKey().toString();
            Object mapping = entry.getValue();
            // 主体映射必须来自上下文；事件和上下文映射允许使用字面量常量。
            Object value = resolve(mapping, ctx, allowConstants);
            if (value == null && !allowConstants) {
                return new MappingResult(result, true);
            }
            if (value != null) {
                result.put(key, value);
            }
        }
        return new MappingResult(result, false);
    }

    /**
     * 解析单个映射表达式，支持 $. 开头的上下文路径和可选字面量。
     */
    private Object resolve(Object mapping, ExecutionContext ctx, boolean allowConstants) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (mapping == null) {
            return null;
        }
        String expression = mapping.toString();
        if (!expression.startsWith("$.")) {
            return allowConstants ? expression : null;
        }
        Object current = ctx.exportContextValues();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String part : expression.substring(2).split("\\.")) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return current;
    }

    /**
     * 基于执行编号、节点编号和重试次数生成风控请求幂等键。
     */
    private String requestId(Map<String, Object> config, ExecutionContext ctx) {
        return ctx.getExecutionId() + ":" + nodeId(config) + ":" + intValue(config.get("attempt"), 1);
    }

    /**
     * 读取节点编号，缺省时使用稳定的风控节点默认值。
     */
    private String nodeId(Map<String, Object> config) {
        String nodeId = string(config.get("nodeId"));
        return nodeId == null || nodeId.isBlank() ? "riskDecision" : nodeId;
    }

    /**
     * 将配置值解析为整数，非法或缺失时返回默认值。
     */
    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    /**
     * 将可空配置值转为字符串。
     */
    private String string(Object value) {
        return value == null ? null : value.toString();
    }

    /**
     * 节点映射结果，包含已解析字段和必要字段缺失标记。
     */
    private record MappingResult(Map<String, Object> values, boolean missingRequired) {
    }
}
