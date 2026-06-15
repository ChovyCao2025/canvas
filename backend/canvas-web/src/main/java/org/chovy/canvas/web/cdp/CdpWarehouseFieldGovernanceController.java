package org.chovy.canvas.web.cdp;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.cdp.api.CdpWarehouseFieldGovernanceFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/warehouse/fields")
public class CdpWarehouseFieldGovernanceController {

    private static final Long DEFAULT_TENANT_ID = 0L;
    private static final String DEFAULT_ACTOR = "system";
    private static final String DEFAULT_ROLE = "OPERATOR";

    private final CdpWarehouseFieldGovernanceFacade facade;

    public CdpWarehouseFieldGovernanceController(CdpWarehouseFieldGovernanceFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/policies")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listPolicies(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String datasetKey,
            @RequestParam(required = false) String lifecycleStatus) {
        return envelope(() -> facade.listPolicies(tenantIdOrDefault(tenantId), datasetKey, lifecycleStatus));
    }

    @PostMapping("/policies")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertPolicy(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.upsertPolicy(tenantIdOrDefault(tenantId), safePayload(payload)));
    }

    @PostMapping("/evaluate-bi-query")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> evaluateBiQuery(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.evaluateBiQuery(tenantIdOrDefault(tenantId), actorOrDefault(actor),
                roleOrDefault(role), safePayload(payload)));
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

    private static String roleOrDefault(String role) {
        return role == null || role.isBlank() ? DEFAULT_ROLE : role.trim();
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
