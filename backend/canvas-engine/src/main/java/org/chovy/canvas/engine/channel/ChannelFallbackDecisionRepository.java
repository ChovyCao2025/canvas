package org.chovy.canvas.engine.channel;

import org.chovy.canvas.dal.dataobject.ChannelFallbackDecisionDO;
import org.chovy.canvas.dal.mapper.ChannelFallbackDecisionMapper;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ChannelFallbackDecisionRepository implements ChannelFallbackService.DecisionRepository {

    private final ChannelFallbackDecisionMapper mapper;

    public ChannelFallbackDecisionRepository(ChannelFallbackDecisionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void insert(ChannelFallbackService.FallbackDecision decision) {
        ChannelFallbackDecisionDO row = new ChannelFallbackDecisionDO();
        row.setTenantId(ChannelConnectorRegistry.tenant(decision.tenantId()));
        row.setExecutionId(decision.executionId());
        row.setNodeId(decision.nodeId());
        row.setOriginalChannel(decision.originalChannel());
        row.setOriginalProvider(decision.originalProvider());
        row.setFinalChannel(decision.finalChannel());
        row.setFinalProvider(decision.finalProvider());
        row.setDecisionReason(decision.reason());
        row.setAttemptChainJson(toJsonArray(decision.attemptChain()));
        mapper.insert(row);
    }

    private static String toJsonArray(Iterable<String> values) {
        return "[" + java.util.stream.StreamSupport.stream(values.spliterator(), false)
                .map(value -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",")) + "]";
    }
}
