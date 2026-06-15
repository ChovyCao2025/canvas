package org.chovy.canvas.web.approvals;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.platform.api.ApprovalFacade;
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
@RequestMapping("/approvals")
public class ApprovalController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";
    private static final String DEFAULT_ROLE = "OPERATOR";

    private final ApprovalFacade facade;

    public ApprovalController(ApprovalFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/tasks")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> tasks(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestParam(defaultValue = "PENDING") String status) {
        return envelope(() -> facade.tasks(tenantIdOrDefault(tenantId), actorOrDefault(actor), roleOrDefault(role),
                status));
    }

    @GetMapping("/instances")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> instances(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String status) {
        return envelope(() -> facade.instances(tenantIdOrDefault(tenantId), targetType, targetId, status));
    }

    @PostMapping("/tasks/{taskId}/approve")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> approve(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @PathVariable Long taskId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.approve(tenantIdOrDefault(tenantId), taskId, safePayload(payload),
                actorOrDefault(actor), roleOrDefault(role)));
    }

    @PostMapping("/tasks/{taskId}/reject")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> reject(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @PathVariable Long taskId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.reject(tenantIdOrDefault(tenantId), taskId, safePayload(payload),
                actorOrDefault(actor), roleOrDefault(role)));
    }

    @PostMapping("/external/lark/sync")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> syncLarkApprovals(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestParam(defaultValue = "100") Integer limit) {
        String safeRole = roleOrDefault(role);
        requireAdmin(safeRole);
        return envelope(() -> facade.syncLarkApprovals(tenantIdOrDefault(tenantId), limit, actorOrDefault(actor),
                safeRole));
    }

    @PostMapping("/external/lark/instances/{instanceId}/sync")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> syncLarkApprovalInstance(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @PathVariable Long instanceId) {
        String safeRole = roleOrDefault(role);
        requireAdmin(safeRole);
        return envelope(() -> facade.syncLarkApprovalInstance(tenantIdOrDefault(tenantId), instanceId,
                actorOrDefault(actor), safeRole));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CompatibilityEnvelope.badRequest(exception.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<CompatibilityEnvelope<Object>> security(SecurityException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(CompatibilityEnvelope.forbidden(exception.getMessage()));
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
        return role == null || role.isBlank() ? DEFAULT_ROLE : role.trim().toUpperCase();
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private static void requireAdmin(String role) {
        if ("ADMIN".equals(role) || "TENANT_ADMIN".equals(role) || "SUPER_ADMIN".equals(role)) {
            return;
        }
        throw new SecurityException("Lark approval sync requires admin role");
    }

    private record CompatibilityEnvelope<T>(int code, String message, String errorCode, T data, String traceId) {
        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static CompatibilityEnvelope<Object> badRequest(String message) {
            return new CompatibilityEnvelope<>(400, message, "API_001", null, null);
        }

        private static CompatibilityEnvelope<Object> forbidden(String message) {
            return new CompatibilityEnvelope<>(403, message, "AUTH_003", null, null);
        }
    }
}
