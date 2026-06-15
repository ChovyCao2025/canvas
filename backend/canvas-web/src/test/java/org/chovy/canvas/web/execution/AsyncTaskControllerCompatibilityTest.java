package org.chovy.canvas.web.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.execution.api.AsyncTaskFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class AsyncTaskControllerCompatibilityTest {

    @Test
    void listParsesCsvBoundsPagingPassesCurrentUserAndWrapsLegacyEnvelope() {
        RecordingFacade facade = new RecordingFacade();

        webClient(facade)
                .get()
                .uri("/canvas/async-tasks?taskType=AUDIENCE_COMPUTE&bizType=AUDIENCE&bizIds= aud-1,,aud-2 &statuses= RUNNING,SUCCEEDED&page=0&size=500")
                .header("X-User", "operator-1")
                .header("X-Role", "OPERATOR")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data[0].taskId").isEqualTo("task-audience-running")
                .jsonPath("$.data[0].progress").isEqualTo(45);

        assertThat(facade.queries).containsExactly(new AsyncTaskFacade.AsyncTaskQuery(
                "AUDIENCE_COMPUTE",
                "AUDIENCE",
                List.of("aud-1", "aud-2"),
                List.of("RUNNING", "SUCCEEDED"),
                "operator-1",
                false,
                1,
                200));
    }

    @Test
    void getUsesDefaultUserAndAdminRoleFlag() {
        RecordingFacade facade = new RecordingFacade();

        webClient(facade)
                .get()
                .uri("/canvas/async-tasks/task-audience-running")
                .header("X-Role", "ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.taskId").isEqualTo("task-audience-running")
                .jsonPath("$.data.status").isEqualTo("RUNNING");

        assertThat(facade.getCalls).containsExactly(new GetCall("task-audience-running", "system", true));
    }

    @Test
    void mapsIllegalArgumentExceptionToApi001BadRequestEnvelope() {
        RecordingFacade facade = new RecordingFacade();
        facade.failure = new IllegalArgumentException("Async task not found: missing");

        webClient(facade)
                .get()
                .uri("/canvas/async-tasks/missing")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Async task not found: missing")
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(AsyncTaskFacade facade) {
        return WebTestClient.bindToController(new AsyncTaskController(facade)).build();
    }

    private static AsyncTaskFacade.AsyncTaskView task(String taskId) {
        return new AsyncTaskFacade.AsyncTaskView(
                taskId,
                "AUDIENCE_COMPUTE",
                "AUDIENCE",
                "aud-1",
                "Compute audience aud-1",
                "RUNNING",
                45,
                null,
                null,
                LocalDateTime.parse("2026-06-15T06:00:00"),
                null,
                LocalDateTime.parse("2026-06-15T05:59:00"),
                LocalDateTime.parse("2026-06-15T06:01:00"),
                "operator-1");
    }

    private static final class RecordingFacade implements AsyncTaskFacade {
        private final List<AsyncTaskQuery> queries = new ArrayList<>();
        private final List<GetCall> getCalls = new ArrayList<>();
        private IllegalArgumentException failure;

        @Override
        public List<AsyncTaskView> listTasks(AsyncTaskQuery query) {
            failIfConfigured();
            queries.add(query);
            return List.of(task("task-audience-running"));
        }

        @Override
        public AsyncTaskView getTask(String taskId, String username, boolean admin) {
            failIfConfigured();
            getCalls.add(new GetCall(taskId, username, admin));
            return task(taskId);
        }

        private void failIfConfigured() {
            if (failure != null) {
                throw failure;
            }
        }
    }

    private record GetCall(String taskId, String username, boolean admin) {
    }
}
