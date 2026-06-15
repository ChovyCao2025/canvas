package org.chovy.canvas.web.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.api.CanvasExecutionFacade;
import org.chovy.canvas.execution.api.CanvasExecutionFacade.ExecutionRequestCommand;
import org.chovy.canvas.execution.api.CanvasExecutionFacade.ExecutionResultView;
import org.chovy.canvas.execution.api.trace.ExecutionTraceView;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
public class ExecutionController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DIRECT_CALL = "DIRECT_CALL";

    private final CanvasExecutionFacade facade;

    public ExecutionController(CanvasExecutionFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/canvas/execute/direct/{canvasId}")
    public Mono<CompatibilityEnvelope<ExecutionResultView>> direct(
            @PathVariable Long canvasId,
            @RequestBody(required = false) DirectExecutionRequest request) {
        return Mono.fromCallable(() -> {
            DirectExecutionRequest body = request == null ? DirectExecutionRequest.empty() : request;
            if (isBlank(body.userId())) {
                throw badRequest("userId is required");
            }
            ExecutionRequestCommand command = new ExecutionRequestCommand(
                    DEFAULT_TENANT_ID,
                    canvasId,
                    null,
                    DIRECT_CALL,
                    body.userId(),
                    body.inputParams(),
                    false);
            return CompatibilityEnvelope.ok(facade.trigger(command));
        });
    }

    @PostMapping("/canvas/execute/dry-run/{canvasId}")
    public Mono<CompatibilityEnvelope<ExecutionResultView>> dryRun(
            @PathVariable Long canvasId,
            @RequestBody(required = false) DirectExecutionRequest request) {
        return Mono.fromCallable(() -> {
            DirectExecutionRequest body = request == null ? DirectExecutionRequest.empty() : request;
            ExecutionRequestCommand command = new ExecutionRequestCommand(
                    DEFAULT_TENANT_ID,
                    canvasId,
                    null,
                    DIRECT_CALL,
                    isBlank(body.userId()) ? "system" : body.userId(),
                    body.inputParams(),
                    true);
            return CompatibilityEnvelope.ok(facade.trigger(command));
        });
    }

    @GetMapping("/canvas/{canvasId}/execution/{executionId}/trace")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> trace(
            @PathVariable Long canvasId,
            @PathVariable String executionId) {
        return Mono.fromCallable(() -> {
            ExecutionTraceView trace = facade.trace(DEFAULT_TENANT_ID, executionId);
            if (trace == null || !canvasId.equals(trace.canvasId())) {
                return CompatibilityEnvelope.ok(List.of());
            }
            return CompatibilityEnvelope.ok(trace.nodeResults().stream()
                    .map(ExecutionController::toOldTraceMap)
                    .toList());
        });
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<CompatibilityEnvelope<Void>> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String message = exception.getReason() == null ? exception.getMessage() : exception.getReason();
        return ResponseEntity
                .status(exception.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(CompatibilityEnvelope.fail("API_001", status, message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CompatibilityEnvelope.fail("API_001", HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private record DirectExecutionRequest(
            String userId,
            Map<String, Object> inputParams,
            String idempotencyKey,
            String graphJson) {

        private DirectExecutionRequest {
            inputParams = Map.copyOf(inputParams == null ? Map.of() : inputParams);
        }

        private static DirectExecutionRequest empty() {
            return new DirectExecutionRequest(null, Map.of(), null, null);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CompatibilityEnvelope<T>(
            int code,
            String message,
            String errorCode,
            T data,
            String traceId) {

        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static <T> CompatibilityEnvelope<T> fail(String errorCode, int code, String message) {
            return new CompatibilityEnvelope<>(code, message, errorCode, null, null);
        }
    }
}
