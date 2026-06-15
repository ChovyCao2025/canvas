package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.chovy.canvas.canvas.api.CanvasCollaborationFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CanvasCollaborationControllerCompatibilityTest {

    private static final Long DEFAULT_TENANT_ID = 7L;

    @Test
    void exposesLegacyCollaborationSummaryRouteThroughFinalController() {
        RecordingCanvasCollaborationFacade facade = new RecordingCanvasCollaborationFacade();

        webClient(facade)
                .get()
                .uri("/canvas/42/collaboration/summary")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.canvasId").isEqualTo(42)
                .jsonPath("$.data.presence[0].userId").isEqualTo("user-1")
                .jsonPath("$.data.presence[0].displayName").isEqualTo("Operator One")
                .jsonPath("$.data.presence[0].state").isEqualTo("online")
                .jsonPath("$.data.activeLockCount").isEqualTo(2)
                .jsonPath("$.data.openCommentCount").isEqualTo(3)
                .jsonPath("$.data.unreadNotificationCount").isEqualTo(4)
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        assertThat(facade.lastTenantId).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(facade.lastCanvasId).isEqualTo(42L);
    }

    @Test
    void usesTenantHeaderWhenProvided() {
        RecordingCanvasCollaborationFacade facade = new RecordingCanvasCollaborationFacade();

        webClient(facade)
                .get()
                .uri("/canvas/42/collaboration/summary")
                .header("X-Tenant-Id", "99")
                .exchange()
                .expectStatus().isOk();

        assertThat(facade.lastTenantId).isEqualTo(99L);
        assertThat(facade.lastCanvasId).isEqualTo(42L);
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingCanvasCollaborationFacade facade = new RecordingCanvasCollaborationFacade();
        facade.failSummary = true;

        webClient(facade)
                .get()
                .uri("/canvas/42/collaboration/summary")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("canvasId is required")
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CanvasCollaborationFacade facade) {
        return WebTestClient.bindToController(new CanvasCollaborationController(facade)).build();
    }

    private static final class RecordingCanvasCollaborationFacade implements CanvasCollaborationFacade {
        private Long lastTenantId;
        private Long lastCanvasId;
        private boolean failSummary;

        @Override
        public Summary summary(Long tenantId, Long canvasId) {
            lastTenantId = tenantId;
            lastCanvasId = canvasId;
            if (failSummary) {
                throw new IllegalArgumentException("canvasId is required");
            }
            return new Summary(
                    canvasId,
                    List.of(new Presence("user-1", "Operator One", "online")),
                    2,
                    3,
                    4);
        }
    }
}
