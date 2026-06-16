package org.chovy.canvas.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasEdgeDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasNodeDefinition;
import org.chovy.canvas.execution.adapter.external.SimpleNodeConfigParser;
import org.junit.jupiter.api.Test;

class DagRuntimeServiceTest {

    @Test
    void buildsRuntimeGraphFromPublishedDefinitionNodesAndEdges() {
        PublishedCanvasDefinition definition = new PublishedCanvasDefinition(
                5L,
                20L,
                21L,
                1,
                "{\"nodes\":[]}",
                Instant.parse("2026-06-10T04:10:00Z"),
                Map.of(),
                List.of(
                        node("start", "START", "{}"),
                        node("decision", "IF_CONDITION", "{\"nextNodeId\":\"ignored-by-edge-view\"}"),
                        node("end", "END", "{}")),
                List.of(
                        edge("start", "decision"),
                        edge("decision", "end")));

        DagGraph graph = new DagRuntimeService(new SimpleNodeConfigParser()).validate(definition);

        assertThat(graph.entryNodes()).containsExactly("start");
        assertThat(graph.downstream("start")).containsExactly("decision");
        assertThat(graph.downstream("decision")).containsExactly("end");
        assertThat(graph.node("decision").nodeType()).isEqualTo("IF_CONDITION");
        assertThat(graph.node("decision").config()).containsEntry("nextNodeId", "ignored-by-edge-view");
    }

    @Test
    void rejectsDuplicateNodesMissingTargetsAndCycles() {
        DagRuntimeService service = new DagRuntimeService();

        assertThatThrownBy(() -> service.validate(definition(
                List.of(node("a", "START", "{}"), node("a", "END", "{}")),
                List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate nodeId");

        assertThatThrownBy(() -> service.validate(definition(
                List.of(node("a", "START", "{}")),
                List.of(edge("a", "missing")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown target");

        assertThatThrownBy(() -> service.validate(definition(
                List.of(node("a", "START", "{}"), node("b", "END", "{}")),
                List.of(edge("a", "b"), edge("b", "a")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cycle");
    }

    private static PublishedCanvasDefinition definition(
            List<PublishedCanvasNodeDefinition> nodes,
            List<PublishedCanvasEdgeDefinition> edges) {
        return new PublishedCanvasDefinition(
                5L,
                20L,
                21L,
                1,
                "{\"nodes\":[]}",
                Instant.parse("2026-06-10T04:10:00Z"),
                Map.of(),
                nodes,
                edges);
    }

    private static PublishedCanvasNodeDefinition node(String nodeId, String nodeType, String configJson) {
        return new PublishedCanvasNodeDefinition(nodeId, nodeType, nodeType, configJson, Map.of(), Map.of());
    }

    private static PublishedCanvasEdgeDefinition edge(String source, String target) {
        return new PublishedCanvasEdgeDefinition(source + "-" + target, source, target, "{}", Map.of());
    }
}
