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
@NodeHandlerType(NodeType.CHANNEL_AVAILABILITY)
public class ChannelAvailabilityHandler implements NodeHandler {

    private final MarketingPolicyService policyService;

    public ChannelAvailabilityHandler(MarketingPolicyService policyService) {
        this.policyService = policyService;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String channel = string(config, "channel", "EMAIL");
        String availableNodeId = string(config, "availableNodeId", string(config, "nextNodeId", null));
        String unavailableNodeId = string(config, "unavailableNodeId", string(config, "suppressedNodeId", null));

        MarketingPolicyService.PolicyDecision decision = policyService.channelAvailable(ctx.getUserId(), channel);
        if (!decision.allowed()) {
            return Mono.just(NodeResult.suppressed(
                    "unavailable", unavailableNodeId, decision.reasonCode(), decision.reasonMessage()));
        }
        return Mono.just(NodeResult.routed("available", availableNodeId, Map.of("channelAvailable", true)));
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
