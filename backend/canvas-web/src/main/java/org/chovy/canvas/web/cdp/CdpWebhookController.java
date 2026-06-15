package org.chovy.canvas.web.cdp;

import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWebhookFacade;
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
@RequestMapping("/cdp/webhooks")
public class CdpWebhookController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final CdpWebhookFacade facade;

    public CdpWebhookController(CdpWebhookFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<Map<String, Object>>> list(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.list(tenantIdOrDefault(tenantId)));
    }

    @PostMapping
    public Mono<CompatibilityEnvelope<Map<String, Object>>> create(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.create(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PutMapping("/{id}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> update(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.update(tenantIdOrDefault(tenantId), id, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PutMapping("/{id}/pause")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> pause(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> facade.pause(tenantIdOrDefault(tenantId), id, actorOrDefault(actor)));
    }

    @PutMapping("/{id}/resume")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> resume(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> facade.resume(tenantIdOrDefault(tenantId), id, actorOrDefault(actor)));
    }

    @DeleteMapping("/{id}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> disable(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> facade.disable(tenantIdOrDefault(tenantId), id, actorOrDefault(actor)));
    }

    @PostMapping("/{id}/rotate-secret")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> rotateSecret(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> facade.rotateSecret(tenantIdOrDefault(tenantId), id, actorOrDefault(actor)));
    }

    @PostMapping("/{id}/test")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> testDelivery(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> facade.testDelivery(tenantIdOrDefault(tenantId), id, actorOrDefault(actor)));
    }

    @GetMapping("/{id}/deliveries")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> deliveries(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long id,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.deliveries(tenantIdOrDefault(tenantId), id, normalizeLimit(limit)));
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

    private static Integer normalizeLimit(Integer limit) {
        return limit == null || limit <= 0 ? 100 : Math.min(limit, 500);
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
