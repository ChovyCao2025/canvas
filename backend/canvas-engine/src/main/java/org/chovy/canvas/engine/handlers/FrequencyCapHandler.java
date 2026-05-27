package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.policy.MarketingPolicyService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * 频控节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.FREQUENCY_CAP)
public class FrequencyCapHandler implements NodeHandler {

    private final MarketingPolicyService policyService;

    public FrequencyCapHandler(MarketingPolicyService policyService) {
        this.policyService = policyService;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String nodeId = string(config, "__nodeId", "frequency-cap");
        String scope = string(config, "scope", "JOURNEY");
        String channel = string(config, "channel", "ALL");
        int maxCount = number(config.get("maxCount"), 1);
        Duration window = duration(
                number(config.get("windowValue"), 1),
                string(config, "windowUnit", "DAYS"));
        String passNodeId = string(config, "passNodeId", string(config, "nextNodeId", null));
        String cappedNodeId = string(config, "cappedNodeId", string(config, "suppressedNodeId", null));

        MarketingPolicyService.PolicyDecision decision = policyService.consumeFrequency(
                ctx.getUserId(), ctx.getCanvasId(), nodeId, scope, channel, maxCount, window);
        if (!decision.allowed()) {
            return Mono.just(NodeResult.suppressed(
                    "capped", cappedNodeId, decision.reasonCode(), decision.reasonMessage()));
        }
        return Mono.just(NodeResult.routed("pass", passNodeId, Map.of(MapFieldKeys.FREQUENCY_ALLOWED, true)));
    }

    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private Duration duration(int amount, String unit) {
        return switch (unit == null ? "DAYS" : unit.toUpperCase()) {
            case "SECOND", "SECONDS" -> Duration.ofSeconds(amount);
            case "MINUTE", "MINUTES" -> Duration.ofMinutes(amount);
            case "HOUR", "HOURS" -> Duration.ofHours(amount);
            default -> Duration.ofDays(amount);
        };
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
