package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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

    private static WebTestClient webClient(CanvasVersionApplicationService service) {
        return webClient(service, new RecordingCanvasPublishService());
    }

    private static WebTestClient webClient(CanvasVersionApplicationService versionService,
                                           CanvasPublishApplicationService publishService) {
        return WebTestClient.bindToController(new CanvasController(versionService, publishService)).build();
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
