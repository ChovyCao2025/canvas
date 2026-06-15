package org.chovy.canvas.web.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.api.ExecutionRequestFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class ExecutionRequestControllerCompatibilityTest {

    @Test
    void mapsLegacyListReplayAndBatchRoutesWithCompatibilityEnvelope() {
        RecordingExecutionRequestFacade facade = new RecordingExecutionRequestFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/canvas/execution-requests?canvasId=42&status=FAILED&userId=user-1&sourceMsgId=msg-1&page=2&size=5")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.total").isEqualTo(1)
                .jsonPath("$.data.list[0].id").isEqualTo("req-1");

        client.post()
                .uri("/canvas/execution-requests/req-1/replay?reason=manual&force=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.requestId").isEqualTo("req-1")
                .jsonPath("$.data.status").isEqualTo("QUEUED")
                .jsonPath("$.data.immediateDispatch").isEqualTo(true);

        client.post()
                .uri("/canvas/execution-requests/replay?canvasId=42&status=FAILED&userId=user-1&sourceMsgId=msg-1&limit=10&reason=batch&force=false")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.count").isEqualTo(1)
                .jsonPath("$.data.requestIds[0]").isEqualTo("req-1")
                .jsonPath("$.data.dispatchFailureCount").isEqualTo(0);

        assertThat(facade.operations).containsExactly("list", "replay", "replayBatch");
        assertThat(facade.lastQuery).isEqualTo(new ExecutionRequestFacade.RequestQuery(
                7L, 42L, "FAILED", "user-1", "msg-1", 2, 5));
        assertThat(facade.lastReplayId).isEqualTo("req-1");
        assertThat(facade.lastReplayCommand).isEqualTo(new ExecutionRequestFacade.ReplayCommand(
                7L, "system", "manual", true));
        assertThat(facade.lastBatchCommand).isEqualTo(new ExecutionRequestFacade.BatchReplayCommand(
                7L, 42L, "FAILED", "user-1", "msg-1", 10, "batch", false));
    }

    @Test
    void omittedPagingAndReplayParamsUseLegacyDefaults() {
        RecordingExecutionRequestFacade facade = new RecordingExecutionRequestFacade();
        WebTestClient client = webClient(facade);

        client.get().uri("/canvas/execution-requests").exchange().expectStatus().isOk();
        client.post().uri("/canvas/execution-requests/req-1/replay").exchange().expectStatus().isOk();
        client.post().uri("/canvas/execution-requests/replay").exchange().expectStatus().isOk();

        assertThat(facade.lastQuery).isEqualTo(new ExecutionRequestFacade.RequestQuery(7L, null, null,
                null, null, 1, 20));
        assertThat(facade.lastReplayCommand).isEqualTo(new ExecutionRequestFacade.ReplayCommand(7L,
                "system", null, false));
        assertThat(facade.lastBatchCommand).isEqualTo(new ExecutionRequestFacade.BatchReplayCommand(7L,
                null, null, null, null, 100, null, false));
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingExecutionRequestFacade facade = new RecordingExecutionRequestFacade();
        facade.failReplay = true;

        webClient(facade).post()
                .uri("/canvas/execution-requests/missing/replay")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("执行请求不存在: missing")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(ExecutionRequestFacade facade) {
        return WebTestClient.bindToController(new ExecutionRequestController(facade)).build();
    }

    private static final class RecordingExecutionRequestFacade implements ExecutionRequestFacade {
        private final List<String> operations = new ArrayList<>();
        private RequestQuery lastQuery;
        private String lastReplayId;
        private ReplayCommand lastReplayCommand;
        private BatchReplayCommand lastBatchCommand;
        private boolean failReplay;

        @Override
        public RequestPageView list(RequestQuery query) {
            operations.add("list");
            lastQuery = query;
            return new RequestPageView(1, query.page(), query.size(), List.of(request("req-1", "FAILED")));
        }

        @Override
        public ReplayResult replay(String id, ReplayCommand command) {
            operations.add("replay");
            lastReplayId = id;
            lastReplayCommand = command;
            if (failReplay) {
                throw new IllegalArgumentException("执行请求不存在: " + id);
            }
            return new ReplayResult(id, "QUEUED", true);
        }

        @Override
        public BatchReplayResult replayBatch(BatchReplayCommand command) {
            operations.add("replayBatch");
            lastBatchCommand = command;
            return new BatchReplayResult(1, command.limit(), List.of("req-1"), 0, List.of());
        }

        private static RequestView request(String id, String status) {
            return new RequestView(id, 7L, 42L, status, "user-1", "msg-1",
                    Map.of("couponCode", "A10"), "2026-06-14T10:00:00", "2026-06-14T10:01:00");
        }
    }
}
