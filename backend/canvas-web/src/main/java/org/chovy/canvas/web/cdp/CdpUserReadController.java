package org.chovy.canvas.web.cdp;

import java.util.List;

import org.chovy.canvas.cdp.api.CdpUserReadFacade;
import org.chovy.canvas.cdp.api.CdpUserReadFacade.CdpUserInsightView;
import org.chovy.canvas.cdp.api.CdpUserReadFacade.CdpUserProfileView;
import org.chovy.canvas.cdp.api.CdpUserReadFacade.CdpUserRowView;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
public class CdpUserReadController {

    private static final Long DEFAULT_TENANT_ID = 7L;

    private final CdpUserReadFacade userReadFacade;

    public CdpUserReadController(CdpUserReadFacade userReadFacade) {
        this.userReadFacade = userReadFacade;
    }

    @GetMapping("/cdp/users")
    public Mono<CompatibilityEnvelope<List<CdpUserRowView>>> listUsers(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String keyword) {
        return envelope(() -> userReadFacade.listUsers(tenantIdOrDefault(tenantId), keyword));
    }

    @GetMapping("/cdp/users/{userId}")
    public Mono<CompatibilityEnvelope<CdpUserProfileView>> getUser(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String userId) {
        return envelope(() -> userReadFacade.getUser(tenantIdOrDefault(tenantId), userId));
    }

    @GetMapping("/cdp/users/{userId}/insight")
    public Mono<CompatibilityEnvelope<CdpUserInsightView>> getInsight(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String userId) {
        return envelope(() -> userReadFacade.getInsight(tenantIdOrDefault(tenantId), userId));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(ThrowingSupplier<T> supplier) {
        return Mono.fromCallable(() -> {
            try {
                return CompatibilityEnvelope.ok(supplier.get());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
            }
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

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
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
