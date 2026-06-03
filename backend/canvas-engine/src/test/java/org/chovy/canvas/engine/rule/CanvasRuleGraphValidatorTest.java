package org.chovy.canvas.engine.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CanvasRuleGraphValidatorTest {

    private final CanvasRuleGraphValidator validator = new CanvasRuleGraphValidator(
            new RuleParser(new ObjectMapper()),
            new RuleValidator());

    @Test
    void rejectsIfConditionWithEmptyRulesUnlessMatchAllIsExplicit() {
        DagParser.CanvasNode ifNode = node("if-1", NodeType.IF_CONDITION, Map.of(
                "rules", List.of(),
                "successNodeId", "coupon",
                "failNodeId", "end"
        ));
        DagGraph graph = graph(Map.of("if-1", ifNode), Map.of("if-1", List.of()));

        assertThatThrownBy(() -> validator.validateOrThrow(graph))
                .isInstanceOf(RuleValidationException.class)
                .hasMessageContaining("if-1")
                .hasMessageContaining("规则不能为空");
    }

    @Test
    void rejectsDirectMergeIntoSideEffectNode() {
        DagParser.CanvasNode sourceA = node("a", NodeType.IF_CONDITION, Map.of("successNodeId", "coupon"));
        DagParser.CanvasNode sourceB = node("b", NodeType.IF_CONDITION, Map.of("successNodeId", "coupon"));
        DagParser.CanvasNode coupon = node("coupon", NodeType.COMMIT_ACTION, Map.of("nextNodeId", "end"));
        DagGraph graph = graph(
                Map.of("a", sourceA, "b", sourceB, "coupon", coupon),
                Map.of("a", List.of(), "b", List.of(), "coupon", List.of("a", "b")));

        assertThatThrownBy(() -> validator.validateOrThrow(graph))
                .isInstanceOf(RuleValidationException.class)
                .hasMessageContaining("coupon")
                .hasMessageContaining("必须先经过汇聚节点");
    }

    @Test
    void rejectsScheduledTriggerWithoutAudienceTaggerDownstream() {
        DagParser.CanvasNode schedule = node("schedule", NodeType.SCHEDULED_TRIGGER, Map.of(
                "scheduleType", "ONCE",
                "triggerTime", "2026-05-29T23:00:00",
                "nextNodeId", "api"
        ));
        DagParser.CanvasNode api = node("api", NodeType.API_CALL, Map.of("nextNodeId", "end"));
        DagGraph graph = graph(
                Map.of("schedule", schedule, "api", api),
                Map.of("schedule", List.of("api"), "api", List.of()),
                Map.of("schedule", List.of(), "api", List.of("schedule")));

        assertThatThrownBy(() -> validator.validateOrThrow(graph))
                .isInstanceOf(RuleValidationException.class)
                .hasMessageContaining("schedule")
                .hasMessageContaining("人群筛选 TAGGER");
    }

    @Test
    void allowsScheduledTriggerToAudienceTaggerDownstream() {
        DagParser.CanvasNode schedule = node("schedule", NodeType.SCHEDULED_TRIGGER, Map.of(
                "scheduleType", "ONCE",
                "triggerTime", "2026-05-29T23:00:00",
                "nextNodeId", "audience"
        ));
        DagParser.CanvasNode audience = node("audience", NodeType.TAGGER, Map.of(
                "mode", "audience",
                "audienceId", 90001,
                "hitNextNodeId", "end"
        ));
        DagGraph graph = graph(
                Map.of("schedule", schedule, "audience", audience),
                Map.of("schedule", List.of("audience"), "audience", List.of()),
                Map.of("schedule", List.of(), "audience", List.of("schedule")));

        validator.validateOrThrow(graph);
    }

    private static DagParser.CanvasNode node(String id, String type, Map<String, Object> config) {
        DagParser.CanvasNode node = new DagParser.CanvasNode();
        node.setId(id);
        node.setName(id);
        node.setType(type);
        node.setConfig(config);
        return node;
    }

    private static DagGraph graph(Map<String, DagParser.CanvasNode> nodes,
                                  Map<String, List<String>> upstream) {
        return graph(nodes, Map.of(), upstream);
    }

    private static DagGraph graph(Map<String, DagParser.CanvasNode> nodes,
                                  Map<String, List<String>> downstream,
                                  Map<String, List<String>> upstream) {
        return new DagGraph(
                nodes,
                downstream,
                upstream,
                nodes.keySet().stream().collect(java.util.stream.Collectors.toMap(id -> id, id -> upstream.getOrDefault(id, List.of()).size()))
        );
    }
}
