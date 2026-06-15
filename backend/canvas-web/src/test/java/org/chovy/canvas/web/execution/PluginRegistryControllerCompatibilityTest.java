package org.chovy.canvas.web.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.api.PluginRegistryFacade;
import org.chovy.canvas.execution.domain.PluginRegistryCatalog.Plugin;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class PluginRegistryControllerCompatibilityTest {

    @Test
    void catalogRouteReturnsLegacyGroupedEnvelope() {
        RecordingPluginRegistryFacade facade = new RecordingPluginRegistryFacade();

        webClient(facade).get().uri("/canvas/plugins")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.action[0].pluginKey").isEqualTo("canvas-plugin-message")
                .jsonPath("$.data.action[0].enabled").isEqualTo(true)
                .jsonPath("$.data.action[0].compatibility.minCanvasVersion").isEqualTo("1.0.0")
                .jsonPath("$.data.action[0].configSchema.required[0]").isEqualTo("templateId");
    }

    @Test
    void setEnabledRouteUsesLegacyHeaderDefaultAndReturnsEmptySuccessEnvelope() {
        RecordingPluginRegistryFacade facade = new RecordingPluginRegistryFacade();

        webClient(facade).put().uri("/canvas/plugins/CANVAS-PLUGIN-MESSAGE/enabled")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"enabled\":false}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").doesNotExist();

        assertThat(facade.commands).containsExactly(new EnableCommand("CANVAS-PLUGIN-MESSAGE", false, "1.0.0"));
    }

    @Test
    void setEnabledRouteMapsValidationFailureToBadRequestEnvelope() {
        RecordingPluginRegistryFacade facade = new RecordingPluginRegistryFacade();
        facade.failure = new IllegalArgumentException("invalid plugin key: ../bad");

        webClient(facade).put().uri("/canvas/plugins/..%2Fbad/enabled")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"enabled\":true}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("invalid plugin key: ../bad");
    }

    private static WebTestClient webClient(PluginRegistryFacade facade) {
        return WebTestClient.bindToController(new PluginRegistryController(facade)).build();
    }

    private record EnableCommand(String pluginKey, boolean enabled, String canvasVersion) {
    }

    private static final class RecordingPluginRegistryFacade implements PluginRegistryFacade {
        private final List<EnableCommand> commands = new ArrayList<>();
        private RuntimeException failure;

        @Override
        public Map<String, List<Plugin>> groupedCatalog() {
            return Map.of("action", List.of(new Plugin(
                    "canvas-plugin-message",
                    "action",
                    "Message",
                    true,
                    Map.of("minCanvasVersion", "1.0.0"),
                    Map.of("required", List.of("templateId")))));
        }

        @Override
        public void setEnabled(String pluginKey, boolean enabled, String canvasVersion) {
            if (failure != null) {
                throw failure;
            }
            commands.add(new EnableCommand(pluginKey, enabled, canvasVersion));
        }
    }
}
