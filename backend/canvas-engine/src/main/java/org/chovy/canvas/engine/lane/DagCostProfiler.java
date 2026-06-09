package org.chovy.canvas.engine.lane;

import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DagCostProfiler {

    private static final int LARGE_RECIPIENT_COUNT = 10_000;
    private static final int LARGE_NODE_COUNT = 50;
    private static final int LARGE_FANOUT_SCORE = 4;

    private static final Set<String> SIDE_EFFECT_NODES = Set.of(
            NodeType.API_CALL,
            NodeType.SEND_MQ,
            NodeType.SEND_MESSAGE,
            NodeType.COMMIT_ACTION,
            NodeType.AI_LLM
    );

    private static final Set<String> WAIT_NODES = Set.of(
            NodeType.WAIT,
            NodeType.USER_INPUT,
            NodeType.MANUAL_APPROVAL,
            NodeType.HUB,
            NodeType.AGGREGATE,
            NodeType.THRESHOLD
    );

    private static final Set<String> HEAVY_NODES = Set.of(
            NodeType.SCHEDULED_TRIGGER,
            NodeType.GROOVY,
            NodeType.TAGGER,
            NodeType.SUB_FLOW_REF,
            NodeType.TRANSFER_JOURNEY
    );

    public CostProfile profile(DagGraph graph,
                               String triggerType,
                               String triggerNodeType,
                               int estimatedRecipientCount) {
        if (graph == null) {
            return new CostProfile(0, 0, 0, 0, 0, 0, Math.max(0, estimatedRecipientCount), ExecutionLane.STANDARD);
        }
        int nodeCount = graph.allNodeIds().size();
        int sideEffectNodeCount = 0;
        int waitNodeCount = 0;
        int fanoutScore = 0;
        int scriptNodeCount = 0;
        int loopRiskScore = 0;
        boolean hasHeavyNode = false;

        for (String nodeId : graph.allNodeIds()) {
            DagParser.CanvasNode node = graph.getNode(nodeId);
            String type = node != null ? node.getType() : null;
            if (SIDE_EFFECT_NODES.contains(type)) {
                sideEffectNodeCount++;
            }
            if (WAIT_NODES.contains(type)) {
                waitNodeCount++;
            }
            if (NodeType.GROOVY.equals(type)) {
                scriptNodeCount++;
            }
            if (NodeType.LOOP.equals(type)) {
                loopRiskScore++;
            }
            if (HEAVY_NODES.contains(type)) {
                hasHeavyNode = true;
            }
            fanoutScore = Math.max(fanoutScore, graph.downstream(nodeId).size());
        }

        int recipients = Math.max(0, estimatedRecipientCount);
        ExecutionLane recommendedLane = recommend(
                triggerType,
                triggerNodeType,
                nodeCount,
                fanoutScore,
                scriptNodeCount,
                loopRiskScore,
                recipients,
                hasHeavyNode);
        return new CostProfile(
                nodeCount,
                sideEffectNodeCount,
                waitNodeCount,
                fanoutScore,
                scriptNodeCount,
                loopRiskScore,
                recipients,
                recommendedLane);
    }

    private ExecutionLane recommend(String triggerType,
                                    String triggerNodeType,
                                    int nodeCount,
                                    int fanoutScore,
                                    int scriptNodeCount,
                                    int loopRiskScore,
                                    int estimatedRecipientCount,
                                    boolean hasHeavyNode) {
        if (TriggerType.DIRECT_CALL.equals(triggerType) || NodeType.DIRECT_CALL.equals(triggerNodeType)) {
            return ExecutionLane.LIGHT;
        }
        if (TriggerType.SCHEDULED.equals(triggerType)
                || hasHeavyNode
                || scriptNodeCount > 0
                || loopRiskScore > 0
                || nodeCount > LARGE_NODE_COUNT
                || fanoutScore >= LARGE_FANOUT_SCORE
                || estimatedRecipientCount >= LARGE_RECIPIENT_COUNT) {
            return ExecutionLane.HEAVY;
        }
        return ExecutionLane.STANDARD;
    }

    public record CostProfile(
            int nodeCount,
            int sideEffectNodeCount,
            int waitNodeCount,
            int fanoutScore,
            int scriptNodeCount,
            int loopRiskScore,
            int estimatedRecipientCount,
            ExecutionLane recommendedLane) {
    }
}
