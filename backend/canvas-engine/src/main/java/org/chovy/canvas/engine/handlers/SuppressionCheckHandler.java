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
 * 营销抑制检查节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.SUPPRESSION_CHECK)
public class SuppressionCheckHandler implements NodeHandler {

    private final MarketingPolicyService policyService;

    public SuppressionCheckHandler(MarketingPolicyService policyService) {
        this.policyService = policyService;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String channel = string(config, "channel", "ALL");
        String allowedNodeId = string(config, "allowedNodeId", string(config, "nextNodeId", null));
        String suppressedNodeId = string(config, "suppressedNodeId", null);
        boolean requireConsent = booleanValue(config.getOrDefault("requireConsent", true));

        MarketingPolicyService.PolicyDecision consent = policyService.consentAllowed(
                ctx.getUserId(), channel, requireConsent);
        if (!consent.allowed()) {
            return Mono.just(NodeResult.suppressed(
                    "suppressed", suppressedNodeId, consent.reasonCode(), consent.reasonMessage()));
        }

        MarketingPolicyService.PolicyDecision suppression = policyService.suppressionAllowed(ctx.getUserId(), channel);
        if (!suppression.allowed()) {
            return Mono.just(NodeResult.suppressed(
                    "suppressed", suppressedNodeId, suppression.reasonCode(), suppression.reasonMessage()));
        }
        return Mono.just(NodeResult.routed("allowed", allowedNodeId, Map.of(MapFieldKeys.POLICY_ALLOWED, true)));
    }

    private boolean booleanValue(Object value) {
        return value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value));
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
