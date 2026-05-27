package org.chovy.canvas.engine.lane;

import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionLaneResolverTest {

    private final ExecutionLaneResolver resolver = new ExecutionLaneResolver();

    @Test
    void resolvesRetryLaneForOverflowOrPersistentRetry() {
        assertThat(resolver.resolve(TriggerType.MQ, NodeType.MQ_TRIGGER, Map.of(), true, false, 0))
                .isEqualTo(ExecutionLane.RETRY);
        assertThat(resolver.resolve(TriggerType.MQ, NodeType.MQ_TRIGGER, Map.of(), false, true, 1))
                .isEqualTo(ExecutionLane.RETRY);
    }

    @Test
    void resolvesLightLaneForDirectAndContinuationTriggers() {
        assertThat(resolver.resolve(TriggerType.DIRECT_CALL, NodeType.DIRECT_CALL, Map.of(), false, false, 0))
                .isEqualTo(ExecutionLane.LIGHT);
        assertThat(resolver.resolve(TriggerType.WAIT_RESUME, NodeType.WAIT, Map.of(), false, false, 0))
                .isEqualTo(ExecutionLane.LIGHT);
    }

    @Test
    void resolvesHeavyLaneForScheduledReplayAndHeavyNodes() {
        assertThat(resolver.resolve(TriggerType.SCHEDULED, NodeType.SCHEDULED_TRIGGER, Map.of(), false, false, 0))
                .isEqualTo(ExecutionLane.HEAVY);
        assertThat(resolver.resolve(TriggerType.EVENT, NodeType.GROOVY, Map.of(), false, false, 0))
                .isEqualTo(ExecutionLane.HEAVY);
    }
}
