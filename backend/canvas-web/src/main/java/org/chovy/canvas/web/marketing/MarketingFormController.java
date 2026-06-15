package org.chovy.canvas.web.marketing;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.marketing.api.MarketingFormFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/canvas/marketing-forms")
public class MarketingFormController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final MarketingFormFacade facade;

    public MarketingFormController(MarketingFormFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> list(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.listForms(tenantIdOrDefault(tenantId)));
    }

    @GetMapping("/{id}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> get(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id) {
        return envelope(() -> facade.getForm(tenantIdOrDefault(tenantId), id));
    }

    @PostMapping
    public Mono<CompatibilityEnvelope<Map<String, Object>>> create(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.createForm(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PutMapping("/{id}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> update(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.updateForm(tenantIdOrDefault(tenantId), id, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PutMapping("/{id}/status")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> setStatus(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.setStatus(tenantIdOrDefault(tenantId), id, safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/submissions")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> submissions(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long formId,
            @RequestParam(defaultValue = "50") Integer limit) {
        return envelope(() -> facade.submissions(tenantIdOrDefault(tenantId), formId, limit));
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

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
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
