package org.chovy.canvas.web.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.api.CanvasExecutionFacade;
import org.chovy.canvas.execution.api.CanvasExecutionFacade.ExecutionRequestCommand;
import org.chovy.canvas.execution.api.CanvasExecutionFacade.ExecutionResultView;
import org.chovy.canvas.execution.api.trace.ExecutionTraceView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class ExecutionControllerCompatibilityTest {

    private static final Long TENANT_ID = 7L;

    @Test
    void directExecutionRoutePreservesSuccessEnvelopeAndMapsCommand() {
        CapturingExecutionFacade facade = new CapturingExecutionFacade(
                new ExecutionResultView("exec-direct-1", "SUCCESS"),
                traceView(42L));

        webClient(facade)
                .post()
                .uri("/canvas/execute/direct/42")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "userId": "user-1",
                          "inputParams": {"couponCode": "A10", "amount": 100},
                          "idempotencyKey": "idem-1"
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
                .jsonPath("$.data.executionId").isEqualTo("exec-direct-1")
                .jsonPath("$.data.status").isEqualTo("SUCCESS");

        assertThat(facade.commands).hasSize(1);
        ExecutionRequestCommand command = facade.commands.getFirst();
        assertThat(command.tenantId()).isEqualTo(TENANT_ID);
        assertThat(command.canvasId()).isEqualTo(42L);
        assertThat(command.versionId()).isNull();
        assertThat(command.triggerType()).isEqualTo("DIRECT_CALL");
        assertThat(command.userId()).isEqualTo("user-1");
        assertThat(command.payload())
                .containsEntry("couponCode", "A10")
                .containsEntry("amount", 100);
        assertThat(command.dryRun()).isFalse();
    }

    @Test
    void directExecutionRouteRejectsBlankOrMissingUserIdBeforeCallingFacade() {
        CapturingExecutionFacade facade = new CapturingExecutionFacade(
                new ExecutionResultView("exec-direct-1", "SUCCESS"),
                traceView(42L));
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/execute/direct/42")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "userId": " ",
                          "inputParams": {"couponCode": "A10"}
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("userId is required");

        client.post()
                .uri("/canvas/execute/direct/42")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "inputParams": {"couponCode": "A10"},
                          "idempotencyKey": "idem-1"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("userId is required");

        assertThat(facade.commands).isEmpty();
    }

    @Test
    void dryRunExecutionRoutePreservesOldPathAndMarksCommandDryRun() {
        CapturingExecutionFacade facade = new CapturingExecutionFacade(
                new ExecutionResultView("dry-run-1", "SUCCESS"),
                traceView(42L));

        webClient(facade)
                .post()
                .uri("/canvas/execute/dry-run/42")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "inputParams": {"couponCode": "A10"},
                          "graphJson": "{\\"nodes\\":[]}"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data.executionId").isEqualTo("dry-run-1")
                .jsonPath("$.data.status").isEqualTo("SUCCESS");

        assertThat(facade.commands).hasSize(1);
        ExecutionRequestCommand command = facade.commands.getFirst();
        assertThat(command.tenantId()).isEqualTo(TENANT_ID);
        assertThat(command.canvasId()).isEqualTo(42L);
        assertThat(command.versionId()).isNull();
        assertThat(command.triggerType()).isEqualTo("DIRECT_CALL");
        assertThat(command.userId()).isEqualTo("system");
        assertThat(command.payload()).containsEntry("couponCode", "A10");
        assertThat(command.dryRun()).isTrue();
    }

    @Test
    void traceRoutePreservesOldPathEnvelopeAndNodeResultKeys() {
        CapturingExecutionFacade facade = new CapturingExecutionFacade(
                new ExecutionResultView("exec-direct-1", "SUCCESS"),
                traceView(42L));

        webClient(facade)
                .get()
                .uri("/canvas/42/execution/exec-direct-1/trace")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data").isArray()
                .jsonPath("$.data.length()").isEqualTo(2)
                .jsonPath("$.data[0].nodeId").isEqualTo("start")
                .jsonPath("$.data[0].nodeType").isEqualTo("START")
                .jsonPath("$.data[0].status").isEqualTo("SUCCESS")
                .jsonPath("$.data[0].errorMsg").isEqualTo("")
                .jsonPath("$.data[0].outputData.accepted").isEqualTo(true)
                .jsonPath("$.data[1].nodeId").isEqualTo("message")
                .jsonPath("$.data[1].nodeType").isEqualTo("MESSAGE")
                .jsonPath("$.data[1].status").isEqualTo("FAILED")
                .jsonPath("$.data[1].errorMsg").isEqualTo("provider timeout")
                .jsonPath("$.data[1].outputData.attempt").isEqualTo(1);

        assertThat(facade.traceRequests).containsExactly(new TraceRequest(TENANT_ID, "exec-direct-1"));
    }

    @Test
    void traceRouteReturnsEmptyDataWhenTraceCanvasDoesNotMatchPathCanvas() {
        CapturingExecutionFacade facade = new CapturingExecutionFacade(
                new ExecutionResultView("exec-direct-1", "SUCCESS"),
                traceView(99L));

        webClient(facade)
                .get()
                .uri("/canvas/42/execution/exec-direct-1/trace")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.data").isArray()
                .jsonPath("$.data.length()").isEqualTo(0);

        assertThat(facade.traceRequests).containsExactly(new TraceRequest(TENANT_ID, "exec-direct-1"));
    }

    @Test
    void illegalArgumentExceptionFromFacadeMapsToApi001BadRequestEnvelope() {
        CapturingExecutionFacade facade = new CapturingExecutionFacade(
                new ExecutionResultView("exec-direct-1", "SUCCESS"),
                traceView(42L));
        facade.triggerFailure = new IllegalArgumentException("canvas is disabled");

        webClient(facade)
                .post()
                .uri("/canvas/execute/direct/42")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "userId": "user-1",
                          "inputParams": {"couponCode": "A10"}
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("canvas is disabled")
                .jsonPath("$.data").doesNotExist();

        assertThat(facade.commands).hasSize(1);
    }

    private static WebTestClient webClient(CanvasExecutionFacade facade) {
        return WebTestClient.bindToController(new ExecutionController(facade)).build();
    }

    private static ExecutionTraceView traceView(Long canvasId) {
        return new ExecutionTraceView(
                TENANT_ID,
                "exec-direct-1",
                canvasId,
                "FAILED",
                Instant.parse("2026-06-12T01:00:00Z"),
                Instant.parse("2026-06-12T01:01:00Z"),
                List.of(
                        new ExecutionTraceView.NodeResultView(
                                "start",
                                "START",
                                "SUCCESS",
                                "",
                                Map.of("accepted", true)),
                        new ExecutionTraceView.NodeResultView(
                                "message",
                                "MESSAGE",
                                "FAILED",
                                "provider timeout",
                                Map.of("attempt", 1))),
                "provider timeout");
    }

    private static final class CapturingExecutionFacade implements CanvasExecutionFacade {
        private final ExecutionResultView triggerResponse;
        private final ExecutionTraceView traceResponse;
        private final List<ExecutionRequestCommand> commands = new ArrayList<>();
        private final List<TraceRequest> traceRequests = new ArrayList<>();
        private RuntimeException triggerFailure;

        private CapturingExecutionFacade(ExecutionResultView triggerResponse, ExecutionTraceView traceResponse) {
            this.triggerResponse = triggerResponse;
            this.traceResponse = traceResponse;
        }

        @Override
        public ExecutionResultView trigger(ExecutionRequestCommand command) {
            commands.add(command);
            if (triggerFailure != null) {
                throw triggerFailure;
            }
            return triggerResponse;
        }

        @Override
        public ExecutionTraceView trace(Long tenantId, String executionId) {
            traceRequests.add(new TraceRequest(tenantId, executionId));
            return traceResponse;
        }
    }

    private record TraceRequest(Long tenantId, String executionId) {
    }
}
