package org.chovy.canvas.canvas.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PublishedCanvasDefinitionTest {

    @Test
    void rejectsMissingRequiredIdentityAndBlankGraphJson() {
        assertThatThrownBy(() -> definition(null, 1L, "{\"nodes\":[]}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");

        assertThatThrownBy(() -> definition(10L, null, "{\"nodes\":[]}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("canvasId");

        assertThatThrownBy(() -> definition(10L, 20L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("graphJson");
    }

    @Test
    void defensivelyCopiesOptionsNodesAndEdges() {
        Map<String, Object> options = new HashMap<>();
        options.put("lane", "standard");
        Map<String, Object> nodeMetadata = new HashMap<>();
        nodeMetadata.put("owner", "growth");
        List<PublishedCanvasNodeDefinition> nodes = new ArrayList<>();
        nodes.add(new PublishedCanvasNodeDefinition(
                "start",
                "TRIGGER",
                "Start",
                "{}",
                Map.of("x", 10, "y", 20),
                nodeMetadata));
        List<PublishedCanvasEdgeDefinition> edges = new ArrayList<>();
        edges.add(new PublishedCanvasEdgeDefinition("e1", "start", "end", "{}", Map.of("label", "ok")));

        PublishedCanvasDefinition definition = new PublishedCanvasDefinition(
                10L,
                20L,
                30L,
                4,
                "{\"nodes\":[]}",
                Instant.parse("2026-06-10T01:00:00Z"),
                options,
                nodes,
                edges);

        options.put("lane", "mutated");
        nodes.clear();
        edges.clear();
        nodeMetadata.put("owner", "mutated");

        assertThat(definition.executionOptions()).containsEntry("lane", "standard");
        assertThat(definition.nodes()).hasSize(1);
        assertThat(definition.edges()).hasSize(1);
        assertThat(definition.nodes().get(0).metadata()).containsEntry("owner", "growth");
        assertThatThrownBy(() -> definition.executionOptions().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> definition.nodes().add(new PublishedCanvasNodeDefinition(
                "other", "MESSAGE", "Other", "{}", Map.of(), Map.of())))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static PublishedCanvasDefinition definition(Long tenantId, Long canvasId, String graphJson) {
        return new PublishedCanvasDefinition(
                tenantId,
                canvasId,
                30L,
                4,
                graphJson,
                Instant.parse("2026-06-10T01:00:00Z"),
                Map.of(),
                List.of(),
                List.of());
    }
}
