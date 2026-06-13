package org.chovy.canvas.web.compat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasDefinitionProvider;
import org.chovy.canvas.canvas.application.dsl.CanvasDslMapper;
import org.chovy.canvas.canvas.application.dsl.CanvasDslValidator;
import org.chovy.canvas.web.canvas.CanvasDslController;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CanvasApiCompatibilityTest {

    @Test
    void validateRoutePreservesValidationEnvelope() {
        webClient().post()
                .uri("/canvas/dsl/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "document": {
                            "apiVersion": "canvas/v1",
                            "kind": "Journey",
                            "metadata": {"name": "new-user-welcome", "title": "New user welcome"},
                            "spec": {
                              "trigger": {"type": "webhook", "event": "user.registered"},
                              "nodes": [{"id": "wait", "type": "delay", "config": {"minutes": 15}}],
                              "edges": []
                            }
                          }
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.valid").isEqualTo(false)
                .jsonPath("$.violations[0].code").isEqualTo("UNSUPPORTED_NODE_TYPE")
                .jsonPath("$.violations[0].message").isEqualTo("Unsupported node type: delay");
    }

    @Test
    void mapRoutePreservesGraphPreviewEnvelope() {
        webClient().post()
                .uri("/canvas/dsl/map")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validDocumentRequest())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.templateKey").isEqualTo("new-user-welcome")
                .jsonPath("$.graphJson").value(graphJson -> assertThat((String) graphJson)
                        .contains("\"dslVersion\":\"canvas/v1\"")
                        .contains("\"kind\":\"Journey\"")
                        .contains("\"name\":\"new-user-welcome\""))
                .jsonPath("$.violations").isArray()
                .jsonPath("$.violations.length()").isEqualTo(0);
    }

    @Test
    void importRoutePreservesValidatedGraphEnvelope() {
        webClient().post()
                .uri("/canvas/dsl/import")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validDocumentRequest())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.importable").isEqualTo(true)
                .jsonPath("$.templateKey").isEqualTo("new-user-welcome")
                .jsonPath("$.graphJson").value(graphJson -> assertThat((String) graphJson)
                        .contains("\"trigger\":{\"type\":\"webhook\",\"event\":\"user.registered\"")
                        .contains("\"nodes\":[{\"id\":\"end\",\"type\":\"end\",\"config\":{}}]"))
                .jsonPath("$.violations.length()").isEqualTo(0);
    }

    @Test
    void exportRoutePreservesPublishedCanvasDslEnvelope() {
        webClient((tenantId, canvasId) -> new PublishedCanvasDefinition(
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
                List.of()))
                .get()
                .uri("/canvas/dsl/export/99")
                .header("X-Tenant-Id", "10")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.exportable").isEqualTo(true)
                .jsonPath("$.canvasId").isEqualTo(99)
                .jsonPath("$.versionId").isEqualTo(100)
                .jsonPath("$.version").isEqualTo(2)
                .jsonPath("$.document.apiVersion").isEqualTo("canvas/v1")
                .jsonPath("$.document.kind").isEqualTo("Journey")
                .jsonPath("$.document.metadata.name").isEqualTo("published-flow")
                .jsonPath("$.document.spec.trigger.event").isEqualTo("lead.created")
                .jsonPath("$.rawGraphJson").doesNotExist()
                .jsonPath("$.violations.length()").isEqualTo(0);
    }

    @Test
    void diffRoutePreservesChangeEnvelope() {
        webClient().post()
                .uri("/canvas/dsl/diff")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "source": {
                            "apiVersion": "canvas/v1",
                            "kind": "Journey",
                            "metadata": {"name": "flow", "title": "Flow"},
                            "spec": {
                              "trigger": {"type": "webhook", "event": "lead.created"},
                              "nodes": [{"id": "end", "type": "end", "config": {}}],
                              "edges": []
                            }
                          },
                          "target": {
                            "apiVersion": "canvas/v1",
                            "kind": "Journey",
                            "metadata": {"name": "flow", "title": "Flow"},
                            "spec": {
                              "trigger": {"type": "webhook", "event": "lead.updated"},
                              "nodes": [{"id": "end", "type": "end", "config": {}}],
                              "edges": []
                            }
                          }
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.changed").isEqualTo(true)
                .jsonPath("$.changes[0].code").isEqualTo("TRIGGER_CHANGED")
                .jsonPath("$.changes[0].path").isEqualTo("trigger")
                .jsonPath("$.changes[0].before").isEqualTo("webhook:lead.created")
                .jsonPath("$.changes[0].after").isEqualTo("webhook:lead.updated")
                .jsonPath("$.sourceViolations.length()").isEqualTo(0)
                .jsonPath("$.targetViolations.length()").isEqualTo(0);
    }

    private static WebTestClient webClient() {
        return webClient((tenantId, canvasId) -> {
            throw new IllegalStateException("Published canvas export provider was not expected");
        });
    }

    private static WebTestClient webClient(PublishedCanvasDefinitionProvider provider) {
        return WebTestClient.bindToController(new CanvasDslController(
                        new CanvasDslValidator(),
                        new CanvasDslMapper(),
                        provider))
                .build();
    }

    private static String validDocumentRequest() {
        return """
                {
                  "document": {
                    "apiVersion": "canvas/v1",
                    "kind": "Journey",
                    "metadata": {"name": "new-user-welcome", "title": "New user welcome"},
                    "spec": {
                      "trigger": {"type": "webhook", "event": "user.registered"},
                      "nodes": [{"id": "end", "type": "end", "config": {}}],
                      "edges": []
                    }
                  }
                }
                """;
    }
}
