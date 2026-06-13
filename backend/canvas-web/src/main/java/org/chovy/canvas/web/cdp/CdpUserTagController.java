package org.chovy.canvas.web.cdp;

import java.util.List;

import org.chovy.canvas.cdp.api.CdpTagFacade;
import org.chovy.canvas.cdp.api.CdpTagWriteCommand;
import org.chovy.canvas.cdp.api.CdpUserTagHistoryView;
import org.chovy.canvas.cdp.api.CdpUserTagView;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
public class CdpUserTagController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String USER_DETAIL_REMOVE_REASON = "user detail remove tag";

    private final CdpTagFacade tagFacade;

    public CdpUserTagController(CdpTagFacade tagFacade) {
        this.tagFacade = tagFacade;
    }

    @PostMapping("/cdp/users/{userId}/tags")
    public Mono<CompatibilityEnvelope<Void>> setTag(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String userId,
            @RequestBody(required = false) CdpTagWriteCommand command) {
        return envelope(() -> {
            CdpTagWriteCommand request = command == null
                    ? new CdpTagWriteCommand(null, null, null, null, null, null, actorOrDefault(actor), null)
                    : command;
            tagFacade.setTag(tenantIdOrDefault(tenantId), userId, request);
            return null;
        });
    }

    @GetMapping("/cdp/users/{userId}/tags")
    public Mono<CompatibilityEnvelope<List<CdpUserTagView>>> listCurrentTags(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String userId) {
        return envelope(() -> tagFacade.listCurrentTags(tenantIdOrDefault(tenantId), userId));
    }

    @GetMapping("/cdp/users/{userId}/tag-history")
    public Mono<CompatibilityEnvelope<List<CdpUserTagHistoryView>>> listHistory(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String userId) {
        return envelope(() -> tagFacade.listHistory(tenantIdOrDefault(tenantId), userId));
    }

    @DeleteMapping("/cdp/users/{userId}/tags/{tagCode}")
    public Mono<CompatibilityEnvelope<Void>> removeTag(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String userId,
            @PathVariable String tagCode) {
        return envelope(() -> {
            tagFacade.removeTag(
                    tenantIdOrDefault(tenantId),
                    userId,
                    tagCode,
                    USER_DETAIL_REMOVE_REASON,
                    actorOrDefault(actor));
            return null;
        });
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
