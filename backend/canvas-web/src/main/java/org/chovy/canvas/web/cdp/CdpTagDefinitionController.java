package org.chovy.canvas.web.cdp;

import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpTagDefinitionFacade;
import org.springframework.http.HttpStatus;
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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/tag-definitions")
public class CdpTagDefinitionController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final CdpTagDefinitionFacade facade;

    public CdpTagDefinitionController(CdpTagDefinitionFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<Map<String, Object>>> list(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String tagType,
            @RequestParam(required = false) Integer enabled) {
        return envelope(() -> facade.list(tenantIdOrDefault(tenantId), page, size, tagType, enabled));
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
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> facade.delete(tenantIdOrDefault(tenantId), id, actorOrDefault(actor)));
    }

    @GetMapping("/{tagCode}/values")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> listValues(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String tagCode,
            @RequestParam(required = false) Integer enabled) {
        return envelope(() -> facade.listValues(tenantIdOrDefault(tenantId), tagCode, enabled));
    }

    @PostMapping("/{tagCode}/values")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createValue(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String tagCode,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.createValue(tenantIdOrDefault(tenantId), tagCode, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PutMapping("/values/{id}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> updateValue(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.updateValue(tenantIdOrDefault(tenantId), id, safePayload(payload),
                actorOrDefault(actor)));
    }

    @DeleteMapping("/values/{id}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> deleteValue(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> facade.deleteValue(tenantIdOrDefault(tenantId), id, actorOrDefault(actor)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(Supplier<T> supplier) {
        return Mono.fromCallable(() -> CompatibilityEnvelope.ok(supplier.get()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }
    }
}
