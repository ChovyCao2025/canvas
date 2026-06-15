package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.application.CanvasCompatibilityApplicationService;
import org.chovy.canvas.canvas.application.CanvasPublishApplicationService;
import org.chovy.canvas.canvas.application.CanvasVersionApplicationService;
import org.chovy.canvas.canvas.domain.CanvasVersion;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CanvasControllerCompatibilityTest {

    @Test
    void listVersionsWrapsPageShapeAndPreservesServiceOrder() {
        RecordingCanvasVersionService service = new RecordingCanvasVersionService();
        service.nextVersions = List.of(
                CanvasVersion.published(11L, 99L, 7L, 3, "{\"nodes\":[\"latest\"]}", "alice"),
                CanvasVersion.draft(10L, 99L, 7L, 2, "{\"nodes\":[\"draft\"]}", "bob"));

        webClient(service)
                .get()
                .uri("/canvas/99/versions")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.total").isEqualTo(2)
                .jsonPath("$.data.list[0].id").isEqualTo(11)
                .jsonPath("$.data.list[0].canvasId").isEqualTo(99)
                .jsonPath("$.data.list[0].tenantId").isEqualTo(7)
                .jsonPath("$.data.list[0].version").isEqualTo(3)
                .jsonPath("$.data.list[0].graphJson").isEqualTo("{\"nodes\":[\"latest\"]}")
                .jsonPath("$.data.list[0].status").isEqualTo("PUBLISHED")
                .jsonPath("$.data.list[0].createdBy").isEqualTo("alice")
                .jsonPath("$.data.list[1].id").isEqualTo(10)
                .jsonPath("$.data.list[1].version").isEqualTo(2)
                .jsonPath("$.data.list[1].createdBy").isEqualTo("bob");

        assertThat(service.canvasId).isEqualTo(99L);
    }

    @Test
    void getVersionWrapsStableVersionFields() {
        RecordingCanvasVersionService service = new RecordingCanvasVersionService();
        service.nextVersion = CanvasVersion.published(
                15L,
                100L,
                8L,
                4,
                "{\"nodes\":[\"published\"]}",
                "operator-1");

        webClient(service)
                .get()
                .uri("/canvas/100/versions/15")
                .header("X-Tenant-Id", "8")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.id").isEqualTo(15)
                .jsonPath("$.data.canvasId").isEqualTo(100)
                .jsonPath("$.data.tenantId").isEqualTo(8)
                .jsonPath("$.data.version").isEqualTo(4)
                .jsonPath("$.data.graphJson").isEqualTo("{\"nodes\":[\"published\"]}")
                .jsonPath("$.data.status").isEqualTo("PUBLISHED")
                .jsonPath("$.data.createdBy").isEqualTo("operator-1");

        assertThat(service.versionId).isEqualTo(15L);
    }

    @Test
    void illegalArgumentExceptionMapsToApi001BadRequestEnvelope() {
        RecordingCanvasVersionService service = new RecordingCanvasVersionService();
        service.failure = new IllegalArgumentException("version is unavailable");

        webClient(service)
                .get()
                .uri("/canvas/100/versions/15")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("version is unavailable")
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    @Test
    void publishMapsToFinalLifecycleServiceAndReturnsStableVersionFields() {
        RecordingCanvasVersionService versionService = new RecordingCanvasVersionService();
        RecordingCanvasPublishService publishService = new RecordingCanvasPublishService();
        publishService.nextPublishedVersion = CanvasVersion.published(
                21L,
                101L,
                9L,
                5,
                "{\"nodes\":[\"publish\"]}",
                "publisher-1");

        webClient(versionService, publishService)
                .post()
                .uri("/canvas/101/publish?operator=publisher-1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.id").isEqualTo(21)
                .jsonPath("$.data.canvasId").isEqualTo(101)
                .jsonPath("$.data.tenantId").isEqualTo(9)
                .jsonPath("$.data.version").isEqualTo(5)
                .jsonPath("$.data.graphJson").isEqualTo("{\"nodes\":[\"publish\"]}")
                .jsonPath("$.data.status").isEqualTo("PUBLISHED")
                .jsonPath("$.data.createdBy").isEqualTo("publisher-1");

        assertThat(publishService.publishedCanvasId).isEqualTo(101L);
        assertThat(publishService.operator).isEqualTo("publisher-1");
    }

    @Test
    void lifecycleRoutesUseDefaultOperatorsAndReturnEmptySuccessEnvelope() {
        RecordingCanvasVersionService versionService = new RecordingCanvasVersionService();
        RecordingCanvasPublishService publishService = new RecordingCanvasPublishService();
        publishService.nextPublishedVersion = CanvasVersion.published(
                22L,
                102L,
                9L,
                6,
                "{\"nodes\":[\"publish\"]}",
                "system");

        webClient(versionService, publishService)
                .post()
                .uri("/canvas/102/publish")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.createdBy").isEqualTo("system");

        webClient(versionService, publishService)
                .post()
                .uri("/canvas/102/offline")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        webClient(versionService, publishService)
                .post()
                .uri("/canvas/103/archive")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data").doesNotExist();

        webClient(versionService, publishService)
                .post()
                .uri("/canvas/104/kill?mode=force")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data").doesNotExist();

        assertThat(publishService.operator).isEqualTo("system");
        assertThat(publishService.unpublishedCanvasId).isEqualTo(102L);
        assertThat(publishService.archivedCanvasId).isEqualTo(103L);
        assertThat(publishService.killedCanvasId).isEqualTo(104L);
    }

    @Test
    void illegalStateExceptionFromPublishMapsToApi001BadRequestEnvelope() {
        RecordingCanvasVersionService versionService = new RecordingCanvasVersionService();
        RecordingCanvasPublishService publishService = new RecordingCanvasPublishService();
        publishService.failure = new IllegalStateException("没有可发布的草稿");

        webClient(versionService, publishService)
                .post()
                .uri("/canvas/101/publish")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("没有可发布的草稿")
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    @Test
    void draftRoutesPreserveCrudListCloneAndImportExportEnvelopes() {
        CanvasCompatibilityApplicationService compatibilityService = new CanvasCompatibilityApplicationService();
        WebTestClient client = webClient(
                new RecordingCanvasVersionService(),
                new RecordingCanvasPublishService(),
                compatibilityService);

        client.post()
                .uri("/canvas")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "welcome", "description", "initial", "graphJson", "{\"nodes\":[]}"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.id").isEqualTo(1000)
                .jsonPath("$.data.tenantId").isEqualTo(7)
                .jsonPath("$.data.createdBy").isEqualTo("operator-1")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();

        client.put()
                .uri("/canvas/1000")
                .header("X-Actor", "editor")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "welcome-v2", "description", "updated", "graphJson", "{\"nodes\":[\"end\"]}"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.name").isEqualTo("welcome-v2")
                .jsonPath("$.data.editVersion").isEqualTo(2)
                .jsonPath("$.data.updatedBy").isEqualTo("editor");

        client.get()
                .uri("/canvas/1000")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.name").isEqualTo("welcome-v2");

        client.get()
                .uri("/canvas/list?page=1&size=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.total").isEqualTo(1)
                .jsonPath("$.data.list[0].id").isEqualTo(1000);

        client.post()
                .uri("/canvas/1000/clone")
                .header("X-Actor", "cloner")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.id").isEqualTo(1001)
                .jsonPath("$.data.name").isEqualTo("welcome-v2 copy")
                .jsonPath("$.data.createdBy").isEqualTo("cloner");

        client.get()
                .uri("/canvas/1000/export?versionId=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.canvasId").isEqualTo(1000)
                .jsonPath("$.data.versionId").isEqualTo(1)
                .jsonPath("$.data.packageJson").value(value -> assertThat((String) value).contains("\"canvasId\":1000"));

        client.post()
                .uri("/canvas/import")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("operator", "importer", "packageJson", "{}"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.created.id").isEqualTo(1002)
                .jsonPath("$.data.created.createdBy").isEqualTo("importer");
    }

    @Test
    void governanceAndOperationsRoutesPreserveCompatibilityEnvelopes() {
        CanvasCompatibilityApplicationService compatibilityService = new CanvasCompatibilityApplicationService();
        WebTestClient client = webClient(
                new RecordingCanvasVersionService(),
                new RecordingCanvasPublishService(),
                compatibilityService);
        compatibilityService.create(7L, "operator-1", Map.of("name", "journey", "graphJson", "{}"));

        client.post()
                .uri("/canvas/1000/submit-review")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("reason", "launch"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("SUBMITTED")
                .jsonPath("$.data.operator").isEqualTo("operator-1");

        client.get()
                .uri("/canvas/1000/approval-status")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("SUBMITTED");

        client.get()
                .uri("/canvas/1000/pre-publish-checks")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.passed").isEqualTo(true)
                .jsonPath("$.data.items[0].code").isEqualTo("GRAPH_PRESENT");

        client.post()
                .uri("/canvas/1000/revert/42")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.versionId").isEqualTo(42);

        client.post()
                .uri("/canvas/1000/canary?percent=30")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.canaryPercent").isEqualTo(30);

        client.post().uri("/canvas/1000/promote-canary").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.status").isEqualTo("PUBLISHED");
        client.post().uri("/canvas/1000/rollback-canary").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.canaryPercent").isEqualTo(0);
        client.post().uri("/canvas/1000/rollback").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.data.status").isEqualTo("ROLLED_BACK");

        client.get()
                .uri("/canvas/1000/versions/1/diff/2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.changed").isEqualTo(true)
                .jsonPath("$.data.changes[0].code").isEqualTo("VERSION_CHANGED");

        client.post()
                .uri("/canvas/1000/message-preview")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("nodeId", "send-1", "userId", "user-1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.nodeId").isEqualTo("send-1")
                .jsonPath("$.data.previewText").isEqualTo("Preview for send-1");
    }

    @Test
    void safeUpdateConflictMapsToLegacyFailureEnvelope() {
        CanvasCompatibilityApplicationService compatibilityService = new CanvasCompatibilityApplicationService();
        WebTestClient client = webClient(
                new RecordingCanvasVersionService(),
                new RecordingCanvasPublishService(),
                compatibilityService);
        compatibilityService.create(7L, "operator-1", Map.of("name", "journey", "graphJson", "{}"));
        compatibilityService.update(7L, "operator-1", 1000L, Map.of("name", "journey-v2"));

        client.put()
                .uri("/canvas/1000/safe")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "stale", "editVersion", 1))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(1)
                .jsonPath("$.message").isEqualTo("画布已被他人修改，请刷新后重试")
                .jsonPath("$.data").doesNotExist();
    }

    @Test
    void templateAndPendingReviewRoutesPreserveLegacyOpsCanvasPaths() {
        CanvasCompatibilityApplicationService compatibilityService = new CanvasCompatibilityApplicationService();
        WebTestClient client = webClient(
                new RecordingCanvasVersionService(),
                new RecordingCanvasPublishService(),
                compatibilityService);
        compatibilityService.create(7L, "operator-1", Map.of(
                "name", "Lifecycle Flow",
                "graphJson", "{\"nodes\":[\"start\"]}"));
        compatibilityService.update(7L, "operator-1", 1000L, Map.of("graphJson", "{\"nodes\":[\"draft\"]}"));

        client.post()
                .uri("/canvas/1000/save-as-template")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Lifecycle Template", "category", "lifecycle"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.id").isEqualTo(5000)
                .jsonPath("$.data.name").isEqualTo("Lifecycle Template")
                .jsonPath("$.data.category").isEqualTo("lifecycle")
                .jsonPath("$.data.graphJson").isEqualTo("{\"nodes\":[\"draft\"]}")
                .jsonPath("$.data.useCount").isEqualTo(0);

        client.get()
                .uri("/canvas/templates?category=lifecycle")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].id").isEqualTo(5000)
                .jsonPath("$.data[0].enabled").isEqualTo(true);

        client.post()
                .uri("/canvas/from-template/5000")
                .header("X-Actor", "creator")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Copied Lifecycle"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.id").isEqualTo(1001)
                .jsonPath("$.data.name").isEqualTo("Copied Lifecycle")
                .jsonPath("$.data.graphJson").isEqualTo("{\"nodes\":[\"draft\"]}")
                .jsonPath("$.data.createdBy").isEqualTo("creator");

        client.post()
                .uri("/canvas/1000/submit-review")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("reason", "launch"))
                .exchange()
                .expectStatus().isOk();

        client.get()
                .uri("/canvas/pending-reviews")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].reviewId").isEqualTo("review-1000")
                .jsonPath("$.data[0].canvasId").isEqualTo(1000)
                .jsonPath("$.data[0].status").isEqualTo("SUBMITTED");
    }

    private static WebTestClient webClient(CanvasVersionApplicationService service) {
        return webClient(service, new RecordingCanvasPublishService());
    }

    private static WebTestClient webClient(CanvasVersionApplicationService versionService,
                                           CanvasPublishApplicationService publishService) {
        return webClient(versionService, publishService, new CanvasCompatibilityApplicationService());
    }

    private static WebTestClient webClient(CanvasVersionApplicationService versionService,
                                           CanvasPublishApplicationService publishService,
                                           CanvasCompatibilityApplicationService compatibilityService) {
        return WebTestClient.bindToController(new CanvasController(
                        versionService,
                        publishService,
                        compatibilityService))
                .build();
    }

    private static final class RecordingCanvasVersionService extends CanvasVersionApplicationService {
        private Long canvasId;
        private Long versionId;
        private List<CanvasVersion> nextVersions = List.of();
        private CanvasVersion nextVersion;
        private RuntimeException failure;

        private RecordingCanvasVersionService() {
            super(null);
        }

        @Override
        public List<CanvasVersion> getVersions(Long canvasId) {
            if (failure != null) {
                throw failure;
            }
            this.canvasId = canvasId;
            return nextVersions;
        }

        @Override
        public CanvasVersion getVersion(Long versionId) {
            if (failure != null) {
                throw failure;
            }
            this.versionId = versionId;
            return nextVersion;
        }
    }

    private static final class RecordingCanvasPublishService extends CanvasPublishApplicationService {
        private Long publishedCanvasId;
        private Long unpublishedCanvasId;
        private Long archivedCanvasId;
        private Long killedCanvasId;
        private String operator;
        private CanvasVersion nextPublishedVersion = CanvasVersion.published(
                1L,
                1L,
                1L,
                1,
                "{}",
                "system");
        private RuntimeException failure;

        private RecordingCanvasPublishService() {
            super(null, null, null);
        }

        @Override
        public CanvasVersion publish(Long canvasId, String operator) {
            if (failure != null) {
                throw failure;
            }
            this.publishedCanvasId = canvasId;
            this.operator = operator;
            return nextPublishedVersion;
        }

        @Override
        public void unpublish(Long canvasId) {
            if (failure != null) {
                throw failure;
            }
            this.unpublishedCanvasId = canvasId;
        }

        @Override
        public void archive(Long canvasId) {
            if (failure != null) {
                throw failure;
            }
            this.archivedCanvasId = canvasId;
        }

        @Override
        public void kill(Long canvasId) {
            if (failure != null) {
                throw failure;
            }
            this.killedCanvasId = canvasId;
        }
    }
}
