package org.chovy.canvas.web.meta;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.execution.api.node.NodeMetadataFacade;
import org.chovy.canvas.execution.api.node.NodeMetadataView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class MetaNodeTypeControllerCompatibilityTest {

    @Test
    void nodeTypeCatalogRouteExposesStableFrontendFieldsFromFinalExecutionMetadata() {
        RecordingNodeMetadataFacade facade = new RecordingNodeMetadataFacade();

        webClient(facade)
                .get()
                .uri("/meta/node-types")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data").isArray()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].nodeType").isEqualTo("message.send")
                .jsonPath("$.data[0].displayName").isEqualTo("Send Message")
                .jsonPath("$.data[0].category").isEqualTo("Messaging")
                .jsonPath("$.data[0].configSchemaJson").isEqualTo("{\"type\":\"object\",\"required\":[\"template\"]}")
                .jsonPath("$.data[0].inputPorts[0]").isEqualTo("in")
                .jsonPath("$.data[0].outputPorts[0]").isEqualTo("success")
                .jsonPath("$.data[0].requiredPluginId").isEqualTo("canvas-plugin-message")
                .jsonPath("$.data[0].enabled").isEqualTo(true)
                .jsonPath("$.data[0].disabledReason").isEqualTo("");

        assertThat(facade.listCalls).isEqualTo(1);
    }

    @Test
    void schemaRouteReturnsOneNodeTypeSchemaByTypeKey() {
        RecordingNodeMetadataFacade facade = new RecordingNodeMetadataFacade();

        webClient(facade)
                .get()
                .uri("/meta/node-types/message.send/schema")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.nodeType").isEqualTo("message.send")
                .jsonPath("$.data.configSchemaJson").isEqualTo("{\"type\":\"object\",\"required\":[\"template\"]}")
                .jsonPath("$.data.inputPorts[0]").isEqualTo("in")
                .jsonPath("$.data.outputPorts[0]").isEqualTo("success");

        assertThat(facade.schemaRequests).containsExactly("message.send");
    }

    @Test
    void unknownTypeMapsToApi001BadRequestEnvelope() {
        RecordingNodeMetadataFacade facade = new RecordingNodeMetadataFacade();

        webClient(facade)
                .get()
                .uri("/meta/node-types/unknown/schema")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("unknown node type: unknown")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(NodeMetadataFacade facade) {
        return WebTestClient.bindToController(new MetaNodeTypeController(facade)).build();
    }

    private static final class RecordingNodeMetadataFacade implements NodeMetadataFacade {
        private final List<String> schemaRequests = new ArrayList<>();
        private int listCalls;

        @Override
        public List<NodeMetadataView> listNodeTypes() {
            listCalls++;
            return List.of(messageNode());
        }

        @Override
        public NodeMetadataView getNodeTypeSchema(String typeKey) {
            schemaRequests.add(typeKey);
            if (!"message.send".equals(typeKey)) {
                throw new IllegalArgumentException("unknown node type: " + typeKey);
            }
            return messageNode();
        }

        private static NodeMetadataView messageNode() {
            return new NodeMetadataView(
                    "message.send",
                    "Send Message",
                    "Messaging",
                    "{\"type\":\"object\",\"required\":[\"template\"]}",
                    List.of("in"),
                    List.of("success"),
                    "canvas-plugin-message",
                    true,
                    "");
        }
    }
}
