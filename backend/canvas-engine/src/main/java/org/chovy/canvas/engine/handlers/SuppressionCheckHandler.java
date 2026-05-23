package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.policy.MarketingPolicyService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

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
        return Mono.just(NodeResult.routed("allowed", allowedNodeId, Map.of("policyAllowed", true)));
    }

    private boolean booleanValue(Object value) {
        return value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value));
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
