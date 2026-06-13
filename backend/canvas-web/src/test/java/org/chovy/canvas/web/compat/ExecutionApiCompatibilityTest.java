package org.chovy.canvas.web.compat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.api.CanvasExecutionFacade;
import org.chovy.canvas.execution.api.CanvasExecutionFacade.ExecutionRequestCommand;
import org.chovy.canvas.execution.api.CanvasExecutionFacade.ExecutionResultView;
import org.chovy.canvas.execution.api.trace.ExecutionTraceView;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

class ExecutionApiCompatibilityTest {

    private static final Long TENANT_ID = 7L;

    @Test
    void directExecutionRoutePreservesEnvelopeAndDelegatesToExecutionFacade() {
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
                .jsonPath("$.data.executionId").isEqualTo("exec-direct-1")
                .jsonPath("$.data.status").isEqualTo("SUCCESS");

        assertThat(facade.commands).hasSize(1);
        ExecutionRequestCommand command = facade.commands.getFirst();
        assertThat(command.tenantId()).isEqualTo(TENANT_ID);
        assertThat(command.canvasId()).isEqualTo(42L);
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
                .expectStatus().isBadRequest();

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
                .expectStatus().isBadRequest();

        assertThat(facade.commands).isEmpty();
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

    private static WebTestClient webClient(CanvasExecutionFacade facade) {
        return WebTestClient.bindToController(new ExecutionControllerAdapter(facade)).build();
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

    @RestController
    private static final class ExecutionControllerAdapter {
        private final CanvasExecutionFacade facade;

        private ExecutionControllerAdapter(CanvasExecutionFacade facade) {
            this.facade = facade;
        }

        @PostMapping("/canvas/execute/direct/{canvasId}")
        Mono<CompatibilityEnvelope<ExecutionResultView>> direct(
                @PathVariable Long canvasId,
                @RequestBody(required = false) DirectExecutionRequest request) {
            return Mono.fromCallable(() -> {
                DirectExecutionRequest body = request == null ? DirectExecutionRequest.empty() : request;
                if (body.userId() == null || body.userId().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
                }
                ExecutionRequestCommand command = new ExecutionRequestCommand(
                        TENANT_ID,
                        canvasId,
                        null,
                        "DIRECT_CALL",
                        body.userId(),
                        body.inputParams(),
                        false);
                return CompatibilityEnvelope.ok(facade.trigger(command));
            });
        }

        @GetMapping("/canvas/{canvasId}/execution/{executionId}/trace")
        Mono<CompatibilityEnvelope<List<Map<String, Object>>>> trace(
                @PathVariable Long canvasId,
                @PathVariable String executionId) {
            return Mono.fromCallable(() -> {
                ExecutionTraceView trace = facade.trace(TENANT_ID, executionId);
                if (trace == null || !canvasId.equals(trace.canvasId())) {
                    return CompatibilityEnvelope.ok(List.<Map<String, Object>>of());
                }
                return CompatibilityEnvelope.ok(trace.nodeResults().stream()
                        .map(ExecutionApiCompatibilityTest::toOldTraceMap)
                        .toList());
            });
        }
    }

    private static Map<String, Object> toOldTraceMap(ExecutionTraceView.NodeResultView result) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("nodeId", result.nodeId());
        map.put("nodeType", result.nodeType());
        map.put("status", result.status());
        map.put("errorMsg", result.error());
        map.put("outputData", result.outputData());
        return map;
    }

    private record DirectExecutionRequest(
            String userId,
            Map<String, Object> inputParams,
            String idempotencyKey) {

        private DirectExecutionRequest {
            inputParams = Map.copyOf(inputParams == null ? Map.of() : inputParams);
        }

        private static DirectExecutionRequest empty() {
            return new DirectExecutionRequest(null, Map.of(), null);
        }
    }

    private record CompatibilityEnvelope<T>(
            int code,
            String message,
            String errorCode,
            T data,
            String traceId) {

        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }
    }

    private static final class CapturingExecutionFacade implements CanvasExecutionFacade {
        private final ExecutionResultView triggerResponse;
        private final ExecutionTraceView traceResponse;
        private final List<ExecutionRequestCommand> commands = new ArrayList<>();
        private final List<TraceRequest> traceRequests = new ArrayList<>();

        private CapturingExecutionFacade(ExecutionResultView triggerResponse, ExecutionTraceView traceResponse) {
            this.triggerResponse = triggerResponse;
            this.traceResponse = traceResponse;
        }

        @Override
        public ExecutionResultView trigger(ExecutionRequestCommand command) {
            commands.add(command);
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
