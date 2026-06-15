package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.canvas.api.CanvasPreferenceFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CanvasPreferenceControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    @Test
    void exposesLegacyEditorPreferenceRoutesThroughFinalController() {
        RecordingCanvasPreferenceFacade facade = new RecordingCanvasPreferenceFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/canvas/preferences/editor")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.preferenceKey").isEqualTo("canvas-editor")
                .jsonPath("$.data.preferenceJson.theme").isEqualTo("system")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        client.put()
                .uri("/canvas/preferences/editor")
                .header("X-Tenant-Id", "42")
                .header("X-Actor", "editor-7")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("theme", "dark"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.preferenceJson.theme").isEqualTo("dark");

        assertThat(facade.getCalls).isEqualTo(1);
        assertThat(facade.upsertCalls).isEqualTo(1);
        assertThat(facade.lastTenantId).isEqualTo(42L);
        assertThat(facade.lastUserId).isEqualTo("editor-7");
        assertThat(facade.lastPatch).containsEntry("theme", "dark");
    }

    @Test
    void defaultsTenantAndActorForEditorPreferenceRoutes() {
        RecordingCanvasPreferenceFacade facade = new RecordingCanvasPreferenceFacade();

        webClient(facade)
                .put()
                .uri("/canvas/preferences/editor")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("sidebarCollapsed", true))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.preferenceJson.sidebarCollapsed").isEqualTo(true);

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastUserId).isEqualTo(DEFAULT_ACTOR);
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingCanvasPreferenceFacade facade = new RecordingCanvasPreferenceFacade();
        facade.failUpsert = true;

        webClient(facade)
                .put()
                .uri("/canvas/preferences/editor")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("unsupported", true))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("unsupported preference key unsupported")
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CanvasPreferenceFacade facade) {
        return WebTestClient.bindToController(new CanvasPreferenceController(facade)).build();
    }

    private static final class RecordingCanvasPreferenceFacade implements CanvasPreferenceFacade {
        private int getCalls;
        private int upsertCalls;
        private Long lastTenantId;
        private String lastUserId;
        private Map<String, Object> lastPatch = Map.of();
        private boolean failUpsert;

        @Override
        public PreferenceView getEditorPreference(Long tenantId, String userId) {
            getCalls++;
            lastTenantId = tenantId;
            lastUserId = userId;
            return preference(Map.of("theme", "system"));
        }

        @Override
        public PreferenceView upsertEditorPreference(Long tenantId, String userId, Map<String, Object> patch) {
            upsertCalls++;
            lastTenantId = tenantId;
            lastUserId = userId;
            lastPatch = new LinkedHashMap<>(patch);
            if (failUpsert) {
                throw new IllegalArgumentException("unsupported preference key unsupported");
            }
            return preference(patch);
        }

        private static PreferenceView preference(Map<String, Object> preferenceJson) {
            return new PreferenceView("canvas-editor", preferenceJson);
        }
    }
}
