package org.chovy.canvas.engine.lane;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DagCostProfilerTest {

    private final DagParser parser = new DagParser(new ObjectMapper());
    private final DagCostProfiler profiler = new DagCostProfiler();

    @Test
    void classifiesLightDirectCallDagAsLight() throws Exception {
        DagCostProfiler.CostProfile profile = profiler.profile(
                graph(List.of(node("start", NodeType.DIRECT_CALL), node("end", NodeType.END)),
                        Map.of("start", List.of("end"))),
                TriggerType.DIRECT_CALL,
                NodeType.DIRECT_CALL,
                1);

        assertThat(profile.recommendedLane()).isEqualTo(ExecutionLane.LIGHT);
        assertThat(profile.nodeCount()).isEqualTo(2);
    }

    @Test
    void classifiesScheduledAudienceDagAsHeavy() throws Exception {
        DagCostProfiler.CostProfile profile = profiler.profile(
                graph(List.of(node("start", NodeType.SCHEDULED_TRIGGER), node("tag", NodeType.TAGGER)),
                        Map.of("start", List.of("tag"))),
                TriggerType.SCHEDULED,
                NodeType.SCHEDULED_TRIGGER,
                100_000);

        assertThat(profile.recommendedLane()).isEqualTo(ExecutionLane.HEAVY);
        assertThat(profile.estimatedRecipientCount()).isEqualTo(100_000);
    }

    @Test
    void classifiesSideEffectHeavyDagAsStandardOrHeavy() throws Exception {
        DagCostProfiler.CostProfile profile = profiler.profile(
                graph(List.of(
                                node("start", NodeType.EVENT_TRIGGER),
                                node("api", NodeType.API_CALL),
                                node("msg", NodeType.SEND_MESSAGE),
                                node("commit", NodeType.COMMIT_ACTION)),
                        Map.of("start", List.of("api"), "api", List.of("msg"), "msg", List.of("commit"))),
                TriggerType.EVENT,
                NodeType.EVENT_TRIGGER,
                200);

        assertThat(profile.sideEffectNodeCount()).isEqualTo(3);
        assertThat(profile.recommendedLane()).isIn(ExecutionLane.STANDARD, ExecutionLane.HEAVY);
    }

    @Test
    void classifiesGroovyScriptDagAsHeavy() throws Exception {
        DagCostProfiler.CostProfile profile = profiler.profile(
                graph(List.of(node("start", NodeType.EVENT_TRIGGER), node("script", NodeType.GROOVY)),
                        Map.of("start", List.of("script"))),
                TriggerType.EVENT,
                NodeType.EVENT_TRIGGER,
                10);

        assertThat(profile.scriptNodeCount()).isEqualTo(1);
        assertThat(profile.recommendedLane()).isEqualTo(ExecutionLane.HEAVY);
    }

    @Test
    void classifiesLargeFanoutDagAsHeavy() throws Exception {
        DagCostProfiler.CostProfile profile = profiler.profile(
                graph(List.of(
                                node("start", NodeType.EVENT_TRIGGER),
                                node("a", NodeType.API_CALL),
                                node("b", NodeType.SEND_MESSAGE),
                                node("c", NodeType.COMMIT_ACTION),
                                node("d", NodeType.WAIT)),
                        Map.of("start", List.of("a", "b", "c", "d"))),
                TriggerType.EVENT,
                NodeType.EVENT_TRIGGER,
                25_000);

        assertThat(profile.fanoutScore()).isGreaterThanOrEqualTo(4);
        assertThat(profile.recommendedLane()).isEqualTo(ExecutionLane.HEAVY);
    }

    @Test
    void addsLoopRiskForGotoDag() throws Exception {
        DagCostProfiler.CostProfile profile = profiler.profile(
                graph(List.of(node("start", NodeType.EVENT_TRIGGER), node("loop", NodeType.LOOP)),
                        Map.of("start", List.of("loop"))),
                TriggerType.EVENT,
                NodeType.EVENT_TRIGGER,
                20);

        assertThat(profile.loopRiskScore()).isGreaterThan(0);
        assertThat(profile.recommendedLane()).isEqualTo(ExecutionLane.HEAVY);
    }

    @Test
    void usesStandardLaneForMissingGraph() {
        DagCostProfiler.CostProfile profile = profiler.profile(null, TriggerType.EVENT, NodeType.EVENT_TRIGGER, 0);

        assertThat(profile.nodeCount()).isZero();
        assertThat(profile.recommendedLane()).isEqualTo(ExecutionLane.STANDARD);
    }

    private DagGraph graph(List<Map<String, Object>> nodes, Map<String, List<String>> edges) throws Exception {
        for (Map.Entry<String, List<String>> edge : edges.entrySet()) {
            Map<String, Object> node = nodes.stream()
                    .filter(candidate -> edge.getKey().equals(candidate.get("id")))
                    .findFirst()
                    .orElseThrow();
            node.put("config", Map.of("branches", edge.getValue().stream()
                    .map(target -> Map.of("nextNodeId", target))
                    .toList()));
        }
        return parser.parse(new ObjectMapper().writeValueAsString(Map.of("nodes", nodes)));
    }

    private Map<String, Object> node(String id, String type) {
        return new java.util.LinkedHashMap<>(Map.of(
                "id", id,
                "type", type,
                "name", id,
                "config", Map.of()));
    }
}
