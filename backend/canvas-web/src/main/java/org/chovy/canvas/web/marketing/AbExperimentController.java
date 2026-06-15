package org.chovy.canvas.web.marketing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.chovy.canvas.marketing.api.AbExperimentFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/canvas/ab-experiments")
public class AbExperimentController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final AbExperimentFacade facade;

    public AbExperimentController(AbExperimentFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<Map<String, Object>>> list(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer enabled) {
        return envelope(() -> facade.list(tenantIdOrDefault(tenantId), query(
                "page", page,
                "size", size,
                "enabled", enabled)));
    }

    @PostMapping
    public Mono<CompatibilityEnvelope<Map<String, Object>>> create(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.create(tenantIdOrDefault(tenantId), safePayload(payload), actorOrDefault(actor)));
    }

    @PutMapping("/{id}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> update(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.update(tenantIdOrDefault(tenantId), id, safePayload(payload),
                actorOrDefault(actor)));
    }

    @DeleteMapping("/{id}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> delete(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id) {
        return envelope(() -> facade.delete(tenantIdOrDefault(tenantId), id));
    }

    @GetMapping("/{id}/groups")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listGroups(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean includeDisabled) {
        return envelope(() -> facade.listGroups(tenantIdOrDefault(tenantId), id, includeDisabled));
    }

    @PostMapping("/{id}/groups")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createGroup(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.createGroup(tenantIdOrDefault(tenantId), id, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PutMapping("/{id}/groups/{groupId}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> updateGroup(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @PathVariable Long groupId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> withDefault(facade.updateGroup(tenantIdOrDefault(tenantId), id, groupId,
                safePayload(payload), actorOrDefault(actor)), "groupId", groupId));
    }

    @DeleteMapping("/{id}/groups/{groupId}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> deleteGroup(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id,
            @PathVariable Long groupId) {
        return envelope(() -> facade.deleteGroup(tenantIdOrDefault(tenantId), id, groupId));
    }

    @PostMapping("/{experimentId}/governance/evaluate")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> evaluateGovernance(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long experimentId,
            @RequestParam(defaultValue = "A") String controlVariantKey) {
        return envelope(() -> facade.evaluateGovernance(tenantIdOrDefault(tenantId), experimentId, controlVariantKey,
                actorOrDefault(actor)));
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

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
    }

    private static Map<String, Object> query(Object... keysAndValues) {
        Map<String, Object> query = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            Object value = keysAndValues[i + 1];
            if (value != null) {
                query.put((String) keysAndValues[i], value);
            }
        }
        return query;
    }

    private static Map<String, Object> withDefault(Map<String, Object> row, String key, Object value) {
        Map<String, Object> copy = safePayload(row);
        copy.putIfAbsent(key, value);
        return copy;
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
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

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get();
    }
}
