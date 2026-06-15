package org.chovy.canvas.web.admin;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.chovy.canvas.platform.api.AdminPlatformFacade;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class AdminPlatformController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final AdminPlatformFacade facade;

    public AdminPlatformController(AdminPlatformFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/admin/users")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> users(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.users(tenantIdOrDefault(tenantId)));
    }

    @PostMapping("/admin/users")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createUser(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.createUser(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PutMapping("/admin/users/{id}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> updateUser(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.updateUser(tenantIdOrDefault(tenantId), id, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PutMapping("/admin/users/{id}/disable")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> disableUser(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> facade.disableUser(tenantIdOrDefault(tenantId), id, actorOrDefault(actor)));
    }

    @GetMapping("/admin/projects")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> projects(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return envelope(() -> facade.projects(tenantIdOrDefault(tenantId)));
    }

    @PostMapping("/admin/projects")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createProject(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.createProject(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/admin/projects/{projectId}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> project(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long projectId) {
        return envelope(() -> facade.project(tenantIdOrDefault(tenantId), projectId));
    }

    @PutMapping("/admin/projects/{projectId}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> updateProject(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.updateProject(tenantIdOrDefault(tenantId), projectId, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PutMapping("/admin/projects/{projectId}/disable")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> disableProject(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long projectId) {
        return envelope(() -> facade.disableProject(tenantIdOrDefault(tenantId), projectId, actorOrDefault(actor)));
    }

    @GetMapping("/admin/projects/{projectId}/members")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> projectMembers(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long projectId) {
        return envelope(() -> facade.projectMembers(tenantIdOrDefault(tenantId), projectId));
    }

    @PutMapping("/admin/projects/{projectId}/members/{userId}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> setProjectMember(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.setProjectMember(tenantIdOrDefault(tenantId), projectId, userId,
                safePayload(payload), actorOrDefault(actor)));
    }

    @DeleteMapping("/admin/projects/{projectId}/members/{userId}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> removeProjectMember(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long projectId,
            @PathVariable Long userId) {
        return envelope(() -> facade.removeProjectMember(tenantIdOrDefault(tenantId), projectId, userId));
    }

    @GetMapping("/admin/projects/{projectId}/canvases")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> projectCanvases(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long projectId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return envelope(() -> facade.projectCanvases(tenantIdOrDefault(tenantId), projectId, page, size));
    }

    @GetMapping("/admin/projects/{projectId}/stats")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> projectStats(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long projectId) {
        return envelope(() -> facade.projectStats(tenantIdOrDefault(tenantId), projectId));
    }

    @GetMapping("/admin/system-options")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> systemOptions(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long tenantIdParam) {
        return envelope(() -> facade.systemOptions(tenantIdOrDefault(tenantId), category, enabled, keyword,
                tenantIdParam));
    }

    @PutMapping("/admin/system-options/{id}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> updateSystemOption(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.updateSystemOption(tenantIdOrDefault(tenantId), id, safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/admin/tenants")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> tenants() {
        return envelope(facade::tenants);
    }

    @PostMapping("/admin/tenants")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createTenant(
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.createTenant(safePayload(payload), actorOrDefault(actor)));
    }

    @PutMapping("/admin/tenants/{id}/disable")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> disableTenant(
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> facade.disableTenant(id, actorOrDefault(actor)));
    }

    @PutMapping("/admin/tenants/{id}/activate")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> activateTenant(
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long id) {
        return envelope(() -> facade.activateTenant(id, actorOrDefault(actor)));
    }

    @GetMapping("/admin/tenants/{id}/usage")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> tenantUsage(@PathVariable Long id) {
        return envelope(() -> facade.tenantUsage(id));
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
