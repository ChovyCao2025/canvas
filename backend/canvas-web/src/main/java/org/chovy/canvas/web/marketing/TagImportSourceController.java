package org.chovy.canvas.web.marketing;

import java.util.Map;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.chovy.canvas.marketing.api.TagImportSourceFacade;
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
@RequestMapping("/canvas/tag-import-sources")
public class TagImportSourceController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final TagImportSourceFacade facade;

    public TagImportSourceController(TagImportSourceFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<Map<String, Object>>> listSources(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Integer enabled) {
        return envelope(() -> facade.listSources(tenantIdOrDefault(tenantId), enabled));
    }

    @PostMapping
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createSource(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.createSource(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PutMapping("/{id}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> updateSource(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.updateSource(tenantIdOrDefault(tenantId), id, safePayload(payload),
                actorOrDefault(actor)));
    }

    @DeleteMapping("/{id}")
    public Mono<CompatibilityEnvelope<Void>> deleteSource(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id) {
        return envelope(() -> {
            facade.deleteSource(tenantIdOrDefault(tenantId), id);
            return null;
        });
    }

    @PostMapping("/{id}/run")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> runSource(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id) {
        return envelope(() -> facade.runSource(tenantIdOrDefault(tenantId), id));
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

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }
    }
}
