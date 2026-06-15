package org.chovy.canvas.web.canvas;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.application.CanvasCompatibilityApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CanvasBatchOperationControllerCompatibilityTest {

    @Test
    void legacyBatchRouteNormalizesOperationAndReturnsPerItemStatuses() {
        CanvasCompatibilityApplicationService service = new CanvasCompatibilityApplicationService();
        service.create(9L, "seed", Map.of("name", "Lifecycle Flow"));

        WebTestClient.bindToController(new CanvasBatchOperationController(service))
                .build()
                .post()
                .uri("/canvas/batch/aRcHiVe")
                .header("X-Tenant-Id", "9")
                .header("X-Actor", "batch-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "canvasIds", List.of(1000L, 404L),
                        "filter", Map.of("status", "DRAFT"),
                        "replacements", Map.of("reason", "cleanup")))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.operation").isEqualTo("ARCHIVE")
                .jsonPath("$.data.totalCount").isEqualTo(2)
                .jsonPath("$.data.successCount").isEqualTo(1)
                .jsonPath("$.data.failedCount").isEqualTo(1)
                .jsonPath("$.data.countsByStatus.SUCCESS").isEqualTo(1)
                .jsonPath("$.data.countsByStatus.FAILED").isEqualTo(1)
                .jsonPath("$.data.items[0].canvasId").isEqualTo(1000)
                .jsonPath("$.data.items[0].status").isEqualTo("SUCCESS")
                .jsonPath("$.data.items[0].message").isEqualTo("ARCHIVED")
                .jsonPath("$.data.items[1].canvasId").isEqualTo(404)
                .jsonPath("$.data.items[1].status").isEqualTo("FAILED")
                .jsonPath("$.data.items[1].message").isEqualTo("画布不存在: 404");
    }

    @Test
    void legacyBatchCloneReturnsNewCanvasIdPerItem() {
        CanvasCompatibilityApplicationService service = new CanvasCompatibilityApplicationService();
        service.create(7L, "seed", Map.of("name", "Clone Source"));

        WebTestClient.bindToController(new CanvasBatchOperationController(service))
                .build()
                .post()
                .uri("/canvas/batch/clone")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("canvasIds", List.of(1000L)))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.operation").isEqualTo("CLONE")
                .jsonPath("$.data.items[0].canvasId").isEqualTo(1000)
                .jsonPath("$.data.items[0].status").isEqualTo("SUCCESS")
                .jsonPath("$.data.items[0].targetCanvasId").isEqualTo(1001);
    }
}
