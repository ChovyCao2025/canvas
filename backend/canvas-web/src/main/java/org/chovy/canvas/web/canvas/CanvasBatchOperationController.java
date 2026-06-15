package org.chovy.canvas.web.canvas;

import java.util.Map;

import org.chovy.canvas.canvas.application.CanvasCompatibilityApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
public class CanvasBatchOperationController {

    private final CanvasCompatibilityApplicationService service;

    public CanvasBatchOperationController(CanvasCompatibilityApplicationService service) {
        this.service = service;
    }

    @PostMapping("/canvas/batch/{operation}")
    public Mono<CompatibilityEnvelope<CanvasCompatibilityApplicationService.BatchOperationView>> run(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String operation,
            @RequestBody(required = false) Map<String, Object> request) {
        return Mono.fromCallable(() -> {
            try {
                return CompatibilityEnvelope.ok(service.batchOperation(tenantId, actor, operation, request));
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
            }
        });
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<CompatibilityEnvelope<Object>> handle(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getReason() == null ? status.getReasonPhrase() : exception.getReason();
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CompatibilityEnvelope.fail("API_001", status.value(), message));
    }

    public record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        static <T> CompatibilityEnvelope<T> fail(String errorCode, int code, String message) {
            return new CompatibilityEnvelope<>(code, message, errorCode, null, null);
        }
    }
}
