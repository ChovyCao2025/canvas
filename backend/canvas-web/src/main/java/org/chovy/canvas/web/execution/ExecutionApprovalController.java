package org.chovy.canvas.web.execution;

import org.chovy.canvas.execution.api.ExecutionApprovalFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
public class ExecutionApprovalController {

    private static final Long DEFAULT_TENANT_ID = 0L;
    private static final String DEFAULT_ACTOR = "system";

    private final ExecutionApprovalFacade approvalFacade;

    public ExecutionApprovalController(ExecutionApprovalFacade approvalFacade) {
        this.approvalFacade = approvalFacade;
    }

    @PostMapping("/canvas/execution/{executionId}/approve")
    public Mono<CompatibilityEnvelope<Void>> approve(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @PathVariable String executionId) {
        return envelope(() -> {
            approvalFacade.approve(tenantIdOrDefault(tenantId), executionId, actorOrDefault(actor), role);
            return null;
        });
    }

    @PostMapping("/canvas/execution/{executionId}/reject")
    public Mono<CompatibilityEnvelope<Void>> reject(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @PathVariable String executionId,
            @RequestParam(required = false) String reason) {
        return envelope(() -> {
            approvalFacade.reject(tenantIdOrDefault(tenantId), executionId, actorOrDefault(actor), reason, role);
            return null;
        });
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(ThrowingSupplier<T> supplier) {
        return Mono.fromCallable(() -> {
            try {
                return CompatibilityEnvelope.ok(supplier.get());
            } catch (SecurityException ex) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage(), ex);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
            }
        });
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<CompatibilityEnvelope<Void>> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String message = exception.getReason() == null ? exception.getMessage() : exception.getReason();
        String errorCode = status == 403 ? "AUTH_003" : "API_001";
        return ResponseEntity
                .status(exception.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(CompatibilityEnvelope.fail(errorCode, status, message));
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

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

    private interface ThrowingSupplier<T> {
        T get();
    }
}
