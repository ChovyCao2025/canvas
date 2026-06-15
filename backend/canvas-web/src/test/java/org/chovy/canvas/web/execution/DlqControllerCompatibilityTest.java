package org.chovy.canvas.web.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.api.DlqFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class DlqControllerCompatibilityTest {

    @Test
    void mapsLegacyDlqRoutesToFacadeWithCompatibilityEnvelope() {
        RecordingDlqFacade facade = new RecordingDlqFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/canvas/dlq?canvasId=42&page=2&size=5")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.total").isEqualTo(1)
                .jsonPath("$.data.list[0].id").isEqualTo(1001)
                .jsonPath("$.data.list[0].canvasId").isEqualTo(42);

        client.post()
                .uri("/canvas/dlq/1001/replay?skipSuccessNodes=false")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.dlqId").isEqualTo(1001)
                .jsonPath("$.data.skipSuccessNodes").isEqualTo(false)
                .jsonPath("$.data.payload.couponCode").isEqualTo("A10");

        client.delete()
                .uri("/canvas/dlq/1001")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.deleted").isEqualTo(true);

        assertThat(facade.operations).containsExactly("list", "replay", "delete");
        assertThat(facade.lastQuery).isEqualTo(new DlqFacade.DlqQuery(42L, 2, 5));
        assertThat(facade.lastReplayId).isEqualTo(1001L);
        assertThat(facade.lastSkipSuccessNodes).isFalse();
        assertThat(facade.lastDeleteId).isEqualTo(1001L);
    }

    @Test
    void replayDefaultsSkipSuccessNodesToTrue() {
        RecordingDlqFacade facade = new RecordingDlqFacade();

        webClient(facade).post()
                .uri("/canvas/dlq/1001/replay")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.skipSuccessNodes").isEqualTo(true);

        assertThat(facade.lastSkipSuccessNodes).isTrue();
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingDlqFacade facade = new RecordingDlqFacade();
        facade.failReplay = true;

        webClient(facade).post()
                .uri("/canvas/dlq/9999/replay")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("DLQ 记录不存在: 9999")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(DlqFacade facade) {
        return WebTestClient.bindToController(new DlqController(facade)).build();
    }

    private static final class RecordingDlqFacade implements DlqFacade {
        private final List<String> operations = new ArrayList<>();
        private DlqQuery lastQuery;
        private Long lastReplayId;
        private Boolean lastSkipSuccessNodes;
        private Long lastDeleteId;
        private boolean failReplay;

        @Override
        public DlqPageView list(DlqQuery query) {
            operations.add("list");
            lastQuery = query;
            return new DlqPageView(1, 2, 5, List.of(entry(1001L, 42L)));
        }

        @Override
        public DlqReplayResult replay(Long id, boolean skipSuccessNodes) {
            operations.add("replay");
            lastReplayId = id;
            lastSkipSuccessNodes = skipSuccessNodes;
            if (failReplay) {
                throw new IllegalArgumentException("DLQ 记录不存在: " + id);
            }
            return new DlqReplayResult(id, 42L, "user-1", "DIRECT_CALL", "DIRECT_CALL",
                    "manual", Map.of("couponCode", "A10"), skipSuccessNodes, "dlq-replay-1001-1");
        }

        @Override
        public DeleteResult delete(Long id) {
            operations.add("delete");
            lastDeleteId = id;
            return new DeleteResult(id, true);
        }

        private static DlqEntryView entry(Long id, Long canvasId) {
            return new DlqEntryView(id, canvasId, "user-1", "DIRECT_CALL", "DIRECT_CALL", "manual",
                    Map.of("couponCode", "A10"), "provider timeout", "2026-06-14T10:00:00");
        }
    }
}
