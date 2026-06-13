package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasDefinitionProvider;
import org.chovy.canvas.canvas.api.dsl.CanvasDslDocument;
import org.chovy.canvas.canvas.application.dsl.CanvasDslMapper;
import org.chovy.canvas.canvas.application.dsl.CanvasDslMappingService;
import org.chovy.canvas.canvas.application.dsl.CanvasDslValidationResult;
import org.chovy.canvas.canvas.application.dsl.CanvasDslValidator;
import org.junit.jupiter.api.Test;

class CanvasDslControllerCompatibilityTest {

    @Test
    void validateEndpointReturnsStableValidationEnvelope() {
        CanvasDslController controller = new CanvasDslController(new CanvasDslValidator(), new CanvasDslMapper());

        CanvasDslController.ValidationResponse response = controller.validate(new CanvasDslController.ValidateRequest(
                new CanvasDslDocument(
                        "canvas/v1",
                        "Journey",
                        new CanvasDslDocument.Metadata("new-user-welcome", "New user welcome"),
                        new CanvasDslDocument.Spec(
                                new CanvasDslDocument.Trigger("webhook", "user.registered"),
                                List.of(new CanvasDslDocument.Node("end", "end", Map.of())),
                                List.of()))));

        assertThat(response.valid()).isTrue();
        assertThat(response.violations()).isEmpty();
    }

    @Test
    void validateEndpointReportsDslViolationsWithoutThrowing() {
        CanvasDslController controller = new CanvasDslController(new CanvasDslValidator(), new CanvasDslMapper());

        CanvasDslController.ValidationResponse response = controller.validate(new CanvasDslController.ValidateRequest(
                new CanvasDslDocument(
                        "canvas/v1",
                        "Journey",
                        new CanvasDslDocument.Metadata("bad", "Bad"),
                        new CanvasDslDocument.Spec(
                                new CanvasDslDocument.Trigger("webhook", "user.registered"),
                                List.of(new CanvasDslDocument.Node("x", "unknown", Map.of())),
                                List.of()))));

        assertThat(response.valid()).isFalse();
        assertThat(response.violations()).extracting(CanvasDslValidationResult.Violation::code)
                .containsExactly("UNSUPPORTED_NODE_TYPE");
    }

    @Test
    void mapEndpointReturnsGraphJsonForCliImportPreview() {
        CanvasDslController controller = new CanvasDslController(new CanvasDslValidator(), new CanvasDslMapper());

        CanvasDslController.MappingResponse response = controller.map(new CanvasDslController.MapRequest(
                new CanvasDslDocument(
                        "canvas/v1",
                        "Journey",
                        new CanvasDslDocument.Metadata("new-user-welcome", "New user welcome"),
                        new CanvasDslDocument.Spec(
                                new CanvasDslDocument.Trigger("webhook", "user.registered"),
                                List.of(new CanvasDslDocument.Node("end", "end", Map.of())),
                                List.of()))));

        assertThat(response.templateKey()).isEqualTo("new-user-welcome");
        assertThat(response.graphJson()).contains("\"dslVersion\":\"canvas/v1\"");
    }

    @Test
    void importEndpointReturnsValidatedGraphJsonEnvelope() {
        CanvasDslController controller = new CanvasDslController(new CanvasDslValidator(), new CanvasDslMapper());

        CanvasDslController.ImportResponse response = controller.importDsl(new CanvasDslController.ImportRequest(
                new CanvasDslDocument(
                        "canvas/v1",
                        "Journey",
                        new CanvasDslDocument.Metadata("new-user-welcome", "New user welcome"),
                        new CanvasDslDocument.Spec(
                                new CanvasDslDocument.Trigger("webhook", "user.registered"),
                                List.of(new CanvasDslDocument.Node("end", "end", Map.of())),
                                List.of()))));

        assertThat(response.importable()).isTrue();
        assertThat(response.templateKey()).isEqualTo("new-user-welcome");
        assertThat(response.graphJson()).contains("\"kind\":\"Journey\"");
        assertThat(response.violations()).isEmpty();
    }

    @Test
    void exportEndpointProjectsPublishedCanvasGraphToDsl() {
        PublishedCanvasDefinitionProvider provider = (tenantId, canvasId) -> new PublishedCanvasDefinition(
                tenantId,
                canvasId,
                100L,
                2,
                """
                        {
                          "metadata": {"name": "published-flow", "title": "Published Flow"},
                          "trigger": {"type": "webhook", "event": "lead.created"},
                          "nodes": [{"id": "end", "type": "end", "config": {}}],
                          "edges": []
                        }
                        """,
                Instant.parse("2026-06-11T00:00:00Z"),
                Map.of(),
                List.of(),
                List.of());
        CanvasDslController controller = new CanvasDslController(new CanvasDslValidator(), new CanvasDslMapper(), provider);

        CanvasDslController.ExportResponse response = controller.exportDsl(10L, 99L);

        assertThat(response.canvasId()).isEqualTo(99L);
        assertThat(response.versionId()).isEqualTo(100L);
        assertThat(response.document().metadata().name()).isEqualTo("published-flow");
        assertThat(response.document().metadata().title()).isEqualTo("Published Flow");
        assertThat(response.document().spec().trigger().event()).isEqualTo("lead.created");
    }

    @Test
    void exportEndpointDoesNotPresentUnsupportedGraphNodesAsValidDsl() {
        PublishedCanvasDefinitionProvider provider = (tenantId, canvasId) -> new PublishedCanvasDefinition(
                tenantId,
                canvasId,
                101L,
                3,
                """
                        {
                          "metadata": {"name": "legacy-flow", "title": "Legacy Flow"},
                          "trigger": {"type": "webhook", "event": "lead.created"},
                          "nodes": [
                            {"id": "wait", "type": "delay", "config": {"minutes": 15}},
                            {"id": "end", "type": "end", "config": {}}
                          ],
                          "edges": [{"from": "wait", "to": "end"}]
                        }
                        """,
                Instant.parse("2026-06-11T00:00:00Z"),
                Map.of(),
                List.of(),
                List.of());
        CanvasDslController controller = new CanvasDslController(new CanvasDslValidator(), new CanvasDslMapper(), provider);

        CanvasDslController.ExportResponse response = controller.exportDsl(10L, 100L);

        assertThat(response.exportable()).isFalse();
        assertThat(response.document()).isNull();
        assertThat(response.rawGraphJson()).contains("\"type\": \"delay\"");
        assertThat(response.violations()).extracting(CanvasDslValidationResult.Violation::code)
                .containsExactly("UNSUPPORTED_NODE_TYPE");
    }

    @Test
    void exportEndpointDoesNotDropUnsupportedEdgeSemantics() {
        PublishedCanvasDefinitionProvider provider = (tenantId, canvasId) -> new PublishedCanvasDefinition(
                tenantId,
                canvasId,
                102L,
                4,
                """
                        {
                          "metadata": {"name": "conditional-flow", "title": "Conditional Flow"},
                          "trigger": {"type": "webhook", "event": "lead.created"},
                          "nodes": [
                            {"id": "condition", "type": "condition", "config": {"field": "country"}},
                            {"id": "end", "type": "end", "config": {}}
                          ],
                          "edges": [
                            {"from": "condition", "to": "end", "conditionJson": {"eq": "US"}}
                          ]
                        }
                        """,
                Instant.parse("2026-06-11T00:00:00Z"),
                Map.of(),
                List.of(),
                List.of());
        CanvasDslController controller = new CanvasDslController(new CanvasDslValidator(), new CanvasDslMapper(), provider);

        CanvasDslController.ExportResponse response = controller.exportDsl(10L, 101L);

        assertThat(response.exportable()).isFalse();
        assertThat(response.document()).isNull();
        assertThat(response.rawGraphJson()).contains("\"conditionJson\"");
        assertThat(response.violations()).extracting(CanvasDslValidationResult.Violation::code)
                .containsExactly("UNSUPPORTED_GRAPH_EDGE_SEMANTICS");
    }

    @Test
    void exportEndpointReturnsRawGraphEnvelopeWhenProjectionFails() {
        PublishedCanvasDefinitionProvider provider = (tenantId, canvasId) -> new PublishedCanvasDefinition(
                tenantId,
                canvasId,
                103L,
                5,
                """
                        {
                          "metadata": {"name": "broken-flow", "title": "Broken Flow"},
                          "trigger": {"type": "webhook", "event": "lead.created"},
                          "nodes": {"id": "end", "type": "end"},
                          "edges": []
                        }
                        """,
                Instant.parse("2026-06-11T00:00:00Z"),
                Map.of(),
                List.of(),
                List.of());
        CanvasDslController controller = new CanvasDslController(new CanvasDslValidator(), new CanvasDslMapper(), provider);

        CanvasDslController.ExportResponse response = controller.exportDsl(10L, 102L);

        assertThat(response.exportable()).isFalse();
        assertThat(response.document()).isNull();
        assertThat(response.rawGraphJson()).contains("\"broken-flow\"");
        assertThat(response.violations()).extracting(CanvasDslValidationResult.Violation::code)
                .containsExactly("UNSUPPORTED_GRAPH_JSON");
    }

    @Test
    void diffEndpointReportsStableChangeEnvelope() {
        CanvasDslController controller = new CanvasDslController(new CanvasDslValidator(), new CanvasDslMapper());
        CanvasDslDocument source = new CanvasDslDocument(
                "canvas/v1",
                "Journey",
                new CanvasDslDocument.Metadata("flow", "Flow"),
                new CanvasDslDocument.Spec(
                        new CanvasDslDocument.Trigger("webhook", "lead.created"),
                        List.of(new CanvasDslDocument.Node("end", "end", Map.of())),
                        List.of()));
        CanvasDslDocument target = new CanvasDslDocument(
                "canvas/v1",
                "Journey",
                new CanvasDslDocument.Metadata("flow", "Flow"),
                new CanvasDslDocument.Spec(
                        new CanvasDslDocument.Trigger("webhook", "lead.updated"),
                        List.of(new CanvasDslDocument.Node("end", "end", Map.of())),
                        List.of()));

        CanvasDslController.DiffResponse response = controller.diff(new CanvasDslController.DiffRequest(source, target));

        assertThat(response.changed()).isTrue();
        assertThat(response.changes()).extracting(CanvasDslMappingService.DiffChange::code)
                .containsExactly("TRIGGER_CHANGED");
    }
}
