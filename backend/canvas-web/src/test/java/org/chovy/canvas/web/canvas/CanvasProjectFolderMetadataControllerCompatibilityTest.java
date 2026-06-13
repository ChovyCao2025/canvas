package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import org.chovy.canvas.canvas.application.CanvasProjectFolderApplicationService;
import org.chovy.canvas.canvas.application.ProjectFolderMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CanvasProjectFolderMetadataControllerCompatibilityTest {

    @Test
    void getMetadataWrapsResponseDefaultsTenantAndHidesTenantId() {
        RecordingProjectFolderService service = new RecordingProjectFolderService();
        service.nextMetadata = new ProjectFolderMetadata(
                99L,
                7L,
                123L,
                "growth",
                "Growth Project",
                "welcome",
                "Welcome Folder");

        webClient(service)
                .get()
                .uri("/canvas/99/project-folder-metadata")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.canvasId").isEqualTo(99)
                .jsonPath("$.data.projectId").isEqualTo(123)
                .jsonPath("$.data.projectKey").isEqualTo("growth")
                .jsonPath("$.data.projectName").isEqualTo("Growth Project")
                .jsonPath("$.data.folderKey").isEqualTo("welcome")
                .jsonPath("$.data.folderName").isEqualTo("Welcome Folder")
                .jsonPath("$.data.tenantId").doesNotExist();

        assertThat(service.getTenantId).isEqualTo(7L);
        assertThat(service.getCanvasId).isEqualTo(99L);
    }

    @Test
    void putMetadataNormalizesActorMapsCommandAndHidesTenantId() {
        RecordingProjectFolderService service = new RecordingProjectFolderService();
        service.nextMetadata = new ProjectFolderMetadata(
                100L,
                42L,
                321L,
                "activation",
                "Activation Project",
                "onboarding",
                "Onboarding Folder");

        webClient(service)
                .put()
                .uri("/canvas/100/project-folder-metadata")
                .header("X-Tenant-Id", "42")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "projectId": 321,
                          "projectKey": " activation ",
                          "projectName": " Activation Project ",
                          "folderKey": " onboarding ",
                          "folderName": " Onboarding Folder ",
                          "operator": "   "
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.canvasId").isEqualTo(100)
                .jsonPath("$.data.projectId").isEqualTo(321)
                .jsonPath("$.data.projectKey").isEqualTo("activation")
                .jsonPath("$.data.projectName").isEqualTo("Activation Project")
                .jsonPath("$.data.folderKey").isEqualTo("onboarding")
                .jsonPath("$.data.folderName").isEqualTo("Onboarding Folder")
                .jsonPath("$.data.tenantId").doesNotExist();

        assertThat(service.saveTenantId).isEqualTo(42L);
        assertThat(service.saveCanvasId).isEqualTo(100L);
        assertThat(service.command).isNotNull();
        assertThat(service.command.projectId()).isEqualTo(321L);
        assertThat(service.command.projectKey()).isEqualTo(" activation ");
        assertThat(service.command.projectName()).isEqualTo(" Activation Project ");
        assertThat(service.command.folderKey()).isEqualTo(" onboarding ");
        assertThat(service.command.folderName()).isEqualTo(" Onboarding Folder ");
        assertThat(service.command.operator()).isEqualTo("operator-1");
    }

    @Test
    void illegalArgumentExceptionMapsToApi001BadRequestEnvelope() {
        RecordingProjectFolderService service = new RecordingProjectFolderService();
        service.failure = new IllegalArgumentException("project folder metadata is invalid");

        webClient(service)
                .get()
                .uri("/canvas/100/project-folder-metadata")
                .header("X-Tenant-Id", "42")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("project folder metadata is invalid")
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CanvasProjectFolderApplicationService service) {
        return WebTestClient.bindToController(new CanvasProjectFolderMetadataController(service)).build();
    }

    private static final class RecordingProjectFolderService extends CanvasProjectFolderApplicationService {
        private Long getTenantId;
        private Long getCanvasId;
        private Long saveTenantId;
        private Long saveCanvasId;
        private SaveProjectFolderCommand command;
        private ProjectFolderMetadata nextMetadata;
        private RuntimeException failure;

        private RecordingProjectFolderService() {
            super(null);
        }

        @Override
        public ProjectFolderMetadata getMetadata(Long tenantId, Long canvasId) {
            if (failure != null) {
                throw failure;
            }
            this.getTenantId = tenantId;
            this.getCanvasId = canvasId;
            return nextMetadata;
        }

        @Override
        public ProjectFolderMetadata saveMetadata(Long tenantId, Long canvasId, SaveProjectFolderCommand command) {
            if (failure != null) {
                throw failure;
            }
            this.saveTenantId = tenantId;
            this.saveCanvasId = canvasId;
            this.command = command;
            return nextMetadata;
        }
    }
}
