package org.chovy.canvas.web.cdp;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWarehouseAudienceFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/warehouse/audiences")
public class CdpWarehouseAudienceController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final CdpWarehouseAudienceFacade facade;

    public CdpWarehouseAudienceController(CdpWarehouseAudienceFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/{audienceId}/materialize")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> materialize(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long audienceId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.materialize(tenantIdOrDefault(tenantId), audienceId,
                actorFrom(payload, actorOrDefault(actor))));
    }

    @PostMapping("/{audienceId}/materialize-gated")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> materializeGated(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long audienceId,
            @RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> body = safePayload(payload);
        return envelope(() -> facade.materializeGated(tenantIdOrDefault(tenantId), audienceId, body,
                actorFrom(body, actorOrDefault(actor))));
    }

    @PostMapping("/{audienceId}/materialize-contract-gated")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> materializeContractGated(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long audienceId,
            @RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> body = safePayload(payload);
        return envelope(() -> facade.materializeContractGated(tenantIdOrDefault(tenantId), audienceId, body,
                actorFrom(body, actorOrDefault(actor))));
    }

    @PostMapping("/{audienceId}/materialization/rollback")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> rollback(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long audienceId,
            @RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> body = safePayload(payload);
        return envelope(() -> facade.rollback(tenantIdOrDefault(tenantId), audienceId, body,
                actorFrom(body, actorOrDefault(actor))));
    }

    @PostMapping("/materialization/refresh-due")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> refreshDue(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> body = safePayload(payload);
        return envelope(() -> facade.refreshDue(tenantIdOrDefault(tenantId), body,
                actorFrom(body, actorOrDefault(actor))));
    }

    @PostMapping("/materialization/refresh-due-gated")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> refreshDueGated(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> body = safePayload(payload);
        return envelope(() -> facade.refreshDueGated(tenantIdOrDefault(tenantId), body,
                actorFrom(body, actorOrDefault(actor))));
    }

    @GetMapping("/materialization-runs")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> recentRuns(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long audienceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") Integer limit) {
        return envelope(() -> facade.recentRuns(tenantIdOrDefault(tenantId), audienceId, status, limit));
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

    private static String actorFrom(Map<String, Object> payload, String fallback) {
        Object operator = payload.get("operator");
        return operator == null || String.valueOf(operator).isBlank() ? fallback : String.valueOf(operator).trim();
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
