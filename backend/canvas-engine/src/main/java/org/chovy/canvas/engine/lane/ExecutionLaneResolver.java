package org.chovy.canvas.engine.lane;

import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class ExecutionLaneResolver {

    private static final Set<String> HEAVY_TRIGGER_TYPES = Set.of(
            TriggerType.SCHEDULED,
            TriggerType.DLQ_REPLAY,
            TriggerType.TRANSFER_JOURNEY,
            TriggerType.SUB_FLOW_REF
    );

    private static final Set<String> LIGHT_TRIGGER_TYPES = Set.of(
            TriggerType.DIRECT_CALL,
            TriggerType.WAIT_RESUME,
            TriggerType.WAIT_TIMEOUT,
            TriggerType.GOAL_CHECK_RESUME,
            TriggerType.GOAL_CHECK_TIMEOUT,
            TriggerType.HUB_TIMEOUT,
            TriggerType.LOGIC_RELATION_TIMEOUT,
            TriggerType.AGGREGATE_TIMEOUT,
            TriggerType.THRESHOLD_TIMEOUT,
            TriggerType.MANUAL_APPROVAL_TIMEOUT,
            "MANUAL_APPROVAL_RESUME"
    );

    private static final Set<String> HEAVY_NODE_TYPES = Set.of(
            NodeType.SCHEDULED_TRIGGER,
            NodeType.GROOVY,
            NodeType.TAGGER_OFFLINE,
            NodeType.TAGGER,
            NodeType.AUDIENCE_TRIGGER,
            NodeType.SUBFLOW,
            NodeType.SUB_FLOW_REF
    );

    private static final Set<String> LIGHT_NODE_TYPES = Set.of(
            NodeType.DIRECT_CALL,
            NodeType.WAIT,
            NodeType.GOAL_CHECK,
            NodeType.HUB,
            NodeType.AGGREGATE,
            NodeType.LOGIC_RELATION,
            NodeType.THRESHOLD,
            NodeType.MANUAL_APPROVAL
    );

    public ExecutionLane resolve(String triggerType,
                                 String triggerNodeType,
                                 Map<String, Object> payload,
                                 boolean overflowRetry,
                                 boolean persistentRequest,
                                 int priorAttemptCount) {
        if (overflowRetry || (persistentRequest && priorAttemptCount > 0)) {
            return ExecutionLane.RETRY;
        }
        if (LIGHT_TRIGGER_TYPES.contains(triggerType) || LIGHT_NODE_TYPES.contains(triggerNodeType)) {
            return ExecutionLane.LIGHT;
        }
        if (HEAVY_TRIGGER_TYPES.contains(triggerType) || HEAVY_NODE_TYPES.contains(triggerNodeType)) {
            return ExecutionLane.HEAVY;
        }
        return ExecutionLane.STANDARD;
    }
}
