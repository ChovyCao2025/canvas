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

import java.util.Map;

/**
 * 静默时段节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.QUIET_HOURS)
public class QuietHoursHandler implements NodeHandler {

    private final MarketingPolicyService policyService;

    public QuietHoursHandler(MarketingPolicyService policyService) {
        this.policyService = policyService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        Map<String, Object> quietHours = config.get("quietHours") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        String start = string(quietHours, "start", string(config, "start", "22:00"));
        String end = string(quietHours, "end", string(config, "end", "08:00"));
        String timezone = string(quietHours, "timezone", string(config, "timezone", "USER_LOCAL"));
        String allowedNodeId = string(config, "allowedNodeId", string(config, "nextNodeId", null));
        String quietNodeId = string(config, "quietNodeId", string(config, "suppressedNodeId", null));

        MarketingPolicyService.PolicyDecision decision = policyService.quietHoursAllowed(
                ctx.getUserId(), start, end, timezone);
        if (!decision.allowed()) {
            return Mono.just(NodeResult.suppressed(
                    "quiet", quietNodeId, decision.reasonCode(), decision.reasonMessage()));
        }
        return Mono.just(NodeResult.routed("allowed", allowedNodeId, Map.of(MapFieldKeys.QUIET_HOURS_ACTIVE, false)));
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
