package org.chovy.canvas.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasEdgeDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasNodeDefinition;
import org.junit.jupiter.api.Test;

class DagParserTest {

    @Test
    void parserReadsPublishedCanvasDefinitionInsteadOfCanvasPersistence() {
        PublishedCanvasDefinition definition = new PublishedCanvasDefinition(
                1L,
                2L,
                3L,
                1,
                "{\"nodes\":[]}",
                Instant.parse("2026-06-10T04:40:00Z"),
                Map.of(),
                List.of(
                        new PublishedCanvasNodeDefinition("start", "START", "Start", "{}", Map.of(), Map.of()),
                        new PublishedCanvasNodeDefinition("end", "END", "End", "{}", Map.of(), Map.of())),
                List.of(new PublishedCanvasEdgeDefinition("e1", "start", "end", "{}", Map.of())));

        DagGraph graph = new DagParser().parse(definition);

        assertThat(graph.entryNodes()).containsExactly("start");
        assertThat(graph.downstream("start")).containsExactly("end");
    }
}
