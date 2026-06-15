package org.chovy.canvas.web.canvas;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.chovy.canvas.canvas.api.CreatorCollaborationFacade;
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
@RequestMapping("/canvas/creator-collaboration")
public class CreatorCollaborationController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final CreatorCollaborationFacade facade;

    public CreatorCollaborationController(CreatorCollaborationFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/creators")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertCreator(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.upsertCreator(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/campaigns")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertCampaign(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.upsertCampaign(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/collaborations")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertCollaboration(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.upsertCollaboration(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/deliverables")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertDeliverable(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.upsertDeliverable(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/mutations")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> proposeMutation(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.proposeMutation(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/mutations/{mutationId}/approve")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> approveMutation(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long mutationId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.approveMutation(tenantIdOrDefault(tenantId), mutationId, safePayload(payload),
                actorOrDefault(actor)));
    }

    @PostMapping("/mutations/{mutationId}/execute")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> executeMutation(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long mutationId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return envelope(() -> facade.executeMutation(tenantIdOrDefault(tenantId), mutationId, safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping("/mutations")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> listMutations(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) Long collaborationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listMutations(tenantIdOrDefault(tenantId), query(
                "campaignId", campaignId,
                "collaborationId", collaborationId,
                "status", status,
                "approvalStatus", approvalStatus,
                "limit", limit)));
    }

    @GetMapping("/summary")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> summary(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) Long collaborationId,
            @RequestParam(required = false) String evaluatedAt) {
        return envelope(() -> facade.summary(tenantIdOrDefault(tenantId), query(
                "campaignId", campaignId,
                "creatorId", creatorId,
                "collaborationId", collaborationId,
                "evaluatedAt", evaluatedAt)));
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
