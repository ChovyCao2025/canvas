package org.chovy.canvas.engine.channel;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

@Component
public class ChannelFallbackService {

    private static final int MAX_CHAIN_DEPTH = 16;

    private final PolicyRepository policies;
    private final DecisionRepository decisions;

    public ChannelFallbackService(PolicyRepository policies, DecisionRepository decisions) {
        this.policies = Objects.requireNonNull(policies, "policies");
        this.decisions = Objects.requireNonNull(decisions, "decisions");
    }

    public FallbackDecision resolve(Long tenantId,
                                    String executionId,
                                    String nodeId,
                                    String channel,
                                    String provider) {
        Route original = Route.of(channel, provider);
        FallbackPolicy policy = policies.find(ChannelConnectorRegistry.tenant(tenantId), original.channel(), original.provider());
        if (policy == null || !policy.enabled()) {
            return new FallbackDecision(
                    ChannelConnectorRegistry.tenant(tenantId),
                    executionId,
                    nodeId,
                    original.channel(),
                    original.provider(),
                    original.channel(),
                    original.provider(),
                    "NO_FALLBACK_POLICY",
                    List.of(original.key()));
        }
        Route fallback = Route.of(policy.fallbackChannel(), policy.fallbackProvider());
        FallbackDecision decision = new FallbackDecision(
                ChannelConnectorRegistry.tenant(tenantId),
                executionId,
                nodeId,
                original.channel(),
                original.provider(),
                fallback.channel(),
                fallback.provider(),
                policy.reason(),
                List.of(original.key(), fallback.key()));
        decisions.insert(decision);
        return decision;
    }

    public void validateNoCycle(Long tenantId, String channel, String provider) {
        validateChain(ChannelConnectorRegistry.tenant(tenantId), Route.of(channel, provider), null);
    }

    public void validateCandidate(Long tenantId,
                                  String channel,
                                  String provider,
                                  String fallbackChannel,
                                  String fallbackProvider) {
        FallbackPolicy proposed = new FallbackPolicy(fallbackChannel, fallbackProvider, true, "VALIDATION");
        validateChain(ChannelConnectorRegistry.tenant(tenantId), Route.of(channel, provider), proposed);
    }

    private void validateChain(Long tenantId, Route start, FallbackPolicy proposed) {
        List<Route> chain = new ArrayList<>();
        Route current = start;
        for (int depth = 0; depth < MAX_CHAIN_DEPTH; depth++) {
            int existingIndex = chain.indexOf(current);
            if (existingIndex >= 0) {
                chain.add(current);
                throw new IllegalArgumentException("fallback cycle: " + joinChain(chain.subList(existingIndex, chain.size())));
            }
            chain.add(current);
            FallbackPolicy policy = findPolicy(tenantId, start, current, proposed);
            if (policy == null || !policy.enabled()) {
                return;
            }
            current = Route.of(policy.fallbackChannel(), policy.fallbackProvider());
        }
        throw new IllegalArgumentException("fallback chain exceeds " + MAX_CHAIN_DEPTH + " hops: " + joinChain(chain));
    }

    private FallbackPolicy findPolicy(Long tenantId, Route start, Route current, FallbackPolicy proposed) {
        if (proposed != null && current.equals(start)) {
            return proposed;
        }
        return policies.find(tenantId, current.channel(), current.provider());
    }

    private static String joinChain(List<Route> chain) {
        StringJoiner joiner = new StringJoiner(" -> ");
        for (Route route : chain) {
            joiner.add(route.key());
        }
        return joiner.toString();
    }

    public interface PolicyRepository {
        FallbackPolicy find(Long tenantId, String channel, String provider);
    }

    public interface DecisionRepository {
        void insert(FallbackDecision decision);
    }

    public record FallbackPolicy(String fallbackChannel, String fallbackProvider, boolean enabled, String reason) {
    }

    public record FallbackDecision(
            Long tenantId,
            String executionId,
            String nodeId,
            String originalChannel,
            String originalProvider,
            String finalChannel,
            String finalProvider,
            String reason,
            List<String> attemptChain) {
    }

    record Route(String channel, String provider) {
        static Route of(String channel, String provider) {
            return new Route(
                    ChannelConnectorRegistry.normalize(channel),
                    ChannelConnectorRegistry.normalizeProvider(provider));
        }

        String key() {
            return channel + ":" + provider;
        }
    }
}
