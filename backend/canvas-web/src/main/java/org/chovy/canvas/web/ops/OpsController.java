package org.chovy.canvas.web.ops;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.platform.api.OpsFacade;
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
@RequestMapping("/ops")
public class OpsController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String DEFAULT_ROLE = "TENANT_ADMIN";

    private final OpsFacade facade;

    public OpsController(OpsFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/cache/invalidate/{id}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> invalidateCache(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> facade.invalidateCache(tenantIdOrDefault(tenantId), id, actorOrDefault(actor)));
    }

    @PostMapping("/recovery/runtime-state/rebuild")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> rebuildRuntimeState(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor) {
        return envelope(() -> facade.rebuildRuntimeState(tenantIdOrDefault(tenantId), actorOrDefault(actor)));
    }

    @GetMapping("/runtime/status")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> runtimeStatus(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role) {
        return envelope(() -> facade.runtimeStatus(tenantIdOrDefault(tenantId), roleOrDefault(role),
                actorOrDefault(actor)));
    }

    @GetMapping("/audit-events")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> auditEvents(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.auditEvents(tenantIdOrDefault(tenantId), normalizeLimit(limit)));
    }

    @PostMapping("/canvas/{id}/pause")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> pauseCanvas(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload) {
        return emergency(tenantId, actor, role, id, "PAUSE", payload);
    }

    @PostMapping("/canvas/{id}/offline")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> offlineCanvas(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload) {
        return emergency(tenantId, actor, role, id, "OFFLINE", payload);
    }

    @PostMapping("/canvas/{id}/resume")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> resumeCanvas(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload) {
        return emergency(tenantId, actor, role, id, "RESUME", payload);
    }

    @PostMapping("/canvas/{id}/kill")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> killCanvas(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload) {
        return emergency(tenantId, actor, role, id, "KILL", payload);
    }

    @PostMapping("/canvas/{id}/rollback")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> rollbackCanvas(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload) {
        return emergency(tenantId, actor, role, id, "ROLLBACK", payload);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    private Mono<CompatibilityEnvelope<Map<String, Object>>> emergency(Long tenantId, String actor, String role,
                                                                       Long canvasId, String action,
                                                                       Map<String, Object> payload) {
        return envelope(() -> facade.emergencyAction(tenantIdOrDefault(tenantId), canvasId, action,
                safePayload(payload), roleOrDefault(role), actorOrDefault(actor)));
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

    private static int normalizeLimit(Integer limit) {
        return limit == null || limit <= 0 ? 50 : Math.min(limit, 500);
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
