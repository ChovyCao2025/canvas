package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.CanvasUserFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class CanvasUserControllerCompatibilityTest {

    @Test
    void mapsLegacyCanvasUserRoutesToFacadeWithCompatibilityEnvelope() {
        RecordingCanvasUserFacade facade = new RecordingCanvasUserFacade();
        WebTestClient client = webClient(facade);

        List<RouteProbe> probes = List.of(
                get("/100/users", "listUsers"),
                get("/100/users/user-1", "getUserInCanvas"),
                get("/100/users/user-1/executions", "listExecutions"));

        for (RouteProbe probe : probes) {
            probe.exchange(client)
                    .expectStatus().isOk()
                    .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(0)
                    .jsonPath("$.message").isEqualTo("success")
                    .jsonPath("$.errorCode").doesNotExist()
                    .jsonPath("$.traceId").doesNotExist();
        }

        assertThat(facade.operations).containsExactlyElementsOf(probes.stream()
                .map(RouteProbe::operation)
                .toList());
    }

    @Test
    void forwardsPathVariablesToFacade() {
        RecordingCanvasUserFacade facade = new RecordingCanvasUserFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/canvas/42/users/user-1/executions")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].canvasId").isEqualTo(42)
                .jsonPath("$.data[0].userId").isEqualTo("user-1");

        assertThat(facade.lastCanvasId).isEqualTo(42L);
        assertThat(facade.lastUserId).isEqualTo("user-1");
    }

    @Test
    void illegalArgumentMapsToApi001BadRequestEnvelope() {
        RecordingCanvasUserFacade facade = new RecordingCanvasUserFacade();
        facade.failGet = true;

        webClient(facade)
                .get()
                .uri("/canvas/42/users/missing")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("canvas user is not found")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(CanvasUserFacade facade) {
        return WebTestClient.bindToController(new CanvasUserController(facade)).build();
    }

    private static RouteProbe get(String path, String operation) {
        return new RouteProbe(path, operation);
    }

    private record RouteProbe(String path, String operation) {
        WebTestClient.ResponseSpec exchange(WebTestClient client) {
            return client.get().uri("/canvas" + path).exchange();
        }
    }

    private static final class RecordingCanvasUserFacade implements CanvasUserFacade {
        private final List<String> operations = new ArrayList<>();
        private Long lastCanvasId;
        private String lastUserId;
        private boolean failGet;

        @Override
        public List<CanvasUserView> listUsers(Long canvasId) {
            operations.add("listUsers");
            lastCanvasId = canvasId;
            return List.of(user(canvasId, "user-1"));
        }

        @Override
        public CanvasUserView getUserInCanvas(Long canvasId, String userId) {
            operations.add("getUserInCanvas");
            lastCanvasId = canvasId;
            lastUserId = userId;
            if (failGet) {
                throw new IllegalArgumentException("canvas user is not found");
            }
            return user(canvasId, userId);
        }

        @Override
        public List<CanvasExecutionView> listExecutions(Long canvasId, String userId) {
            operations.add("listExecutions");
            lastCanvasId = canvasId;
            lastUserId = userId;
            return List.of(new CanvasExecutionView(1L, canvasId, userId, 10L, "send_sms", "SUCCESS",
                    LocalDateTime.parse("2026-06-14T10:00:00")));
        }

        @Override
        public void registerUser(Long canvasId, CanvasUserCommand command) {
        }

        @Override
        public void registerExecution(Long canvasId, String userId, ExecutionCommand command) {
        }

        private static CanvasUserView user(Long canvasId, String userId) {
            return new CanvasUserView(canvasId, userId, "buyer@example.com", "13800000000", "ENTERED",
                    Map.of("tier", "gold"));
        }
    }
}
