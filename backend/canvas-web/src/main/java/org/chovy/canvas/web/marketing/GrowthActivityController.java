package org.chovy.canvas.web.marketing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.GrowthActivityFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/canvas/growth-activities")
public class GrowthActivityController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final GrowthActivityFacade facade;

    public GrowthActivityController(GrowthActivityFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertActivity(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return envelope(() -> facade.upsertActivity(tenantIdOrDefault(tenantId), safePayload(payload),
                actorOrDefault(actor)));
    }

    @GetMapping
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> listActivities(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return envelope(() -> facade.listActivities(tenantIdOrDefault(tenantId), activityType, status, limit));
    }

    @GetMapping("/{activityId}")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> getActivity(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long activityId) {
        return envelope(() -> facade.getActivity(tenantIdOrDefault(tenantId), activityId));
    }

    @GetMapping("/{activityId}/report")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> report(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long activityId) {
        return envelope(() -> facade.report(tenantIdOrDefault(tenantId), activityId));
    }

    @GetMapping("/{activityId}/readiness")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> readiness(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long activityId) {
        return envelope(() -> facade.readiness(tenantIdOrDefault(tenantId), activityId));
    }

    @GetMapping("/{activityId}/reward-pools")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> rewardPools(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long activityId) {
        return list(tenantId, activityId, "rewardPools", Map.of(), null);
    }

    @PostMapping("/{activityId}/reward-pools")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertRewardPool(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long activityId,
            @RequestBody Map<String, Object> payload) {
        return execute(tenantId, actor, activityId, "upsertRewardPool", payload);
    }

    @GetMapping("/{activityId}/grants")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> grants(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long activityId) {
        return list(tenantId, activityId, "grants", Map.of(), null);
    }

    @PostMapping("/{activityId}/grants")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> createGrant(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long activityId,
            @RequestBody Map<String, Object> payload) {
        return execute(tenantId, actor, activityId, "createGrant", payload);
    }

    @PostMapping("/{activityId}/grants/{grantId}/retry")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> retryGrant(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long activityId,
            @PathVariable Long grantId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return execute(tenantId, actor, activityId, "retryGrant", with(payload, "grantId", grantId));
    }

    @PostMapping("/{activityId}/grants/{grantId}/reconcile")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> reconcileGrant(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long activityId,
            @PathVariable Long grantId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return execute(tenantId, actor, activityId, "reconcileGrant", with(payload, "grantId", grantId));
    }

    @PostMapping("/{activityId}/grants/{grantId}/cancel")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> cancelGrant(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long activityId,
            @PathVariable Long grantId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return execute(tenantId, actor, activityId, "cancelGrant", with(payload, "grantId", grantId));
    }

    @GetMapping("/{activityId}/referral-codes")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> referralCodes(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long activityId) {
        return list(tenantId, activityId, "referralCodes", Map.of(), null);
    }

    @PostMapping("/{activityId}/referral-codes")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> generateReferralCode(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long activityId,
            @RequestBody Map<String, Object> payload) {
        return execute(tenantId, actor, activityId, "generateReferralCode", payload);
    }

    @GetMapping("/{activityId}/referrals")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> referrals(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long activityId) {
        return list(tenantId, activityId, "referrals", Map.of(), null);
    }

    @PostMapping("/{activityId}/referrals")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertReferral(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long activityId,
            @RequestBody Map<String, Object> payload) {
        return execute(tenantId, actor, activityId, "upsertReferral", payload);
    }

    @PostMapping("/{activityId}/referrals/{relationId}/qualify")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> qualifyReferral(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long activityId,
            @PathVariable Long relationId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return execute(tenantId, actor, activityId, "qualifyReferral", with(payload, "relationId", relationId));
    }

    @GetMapping("/{activityId}/tasks")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> tasks(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long activityId) {
        return list(tenantId, activityId, "tasks", Map.of(), null);
    }

    @PostMapping("/{activityId}/tasks")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertTask(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long activityId,
            @RequestBody Map<String, Object> payload) {
        return execute(tenantId, actor, activityId, "upsertTask", payload);
    }

    @GetMapping("/{activityId}/task-progress")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> taskProgress(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long activityId) {
        return list(tenantId, activityId, "taskProgress", Map.of(), null);
    }

    @PostMapping("/{activityId}/task-progress")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> recordTaskProgress(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long activityId,
            @RequestBody Map<String, Object> payload) {
        return execute(tenantId, actor, activityId, "recordTaskProgress", payload);
    }

    @PostMapping("/{activityId}/task-progress/{progressId}/reset")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> resetTaskProgress(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long activityId,
            @PathVariable Long progressId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return execute(tenantId, actor, activityId, "resetTaskProgress", with(payload, "progressId", progressId));
    }

    @PostMapping("/{activityId}/publish")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> publish(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long activityId) {
        return transition(tenantId, actor, activityId, "publish");
    }

    @PostMapping("/{activityId}/pause")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> pause(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long activityId) {
        return transition(tenantId, actor, activityId, "pause");
    }

    @PostMapping("/{activityId}/close")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> close(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long activityId) {
        return transition(tenantId, actor, activityId, "close");
    }

    private Mono<CompatibilityEnvelope<Map<String, Object>>> execute(Long tenantId,
                                                                     String actor,
                                                                     Long activityId,
                                                                     String operation,
                                                                     Map<String, Object> payload) {
        return envelope(() -> facade.execute(tenantIdOrDefault(tenantId), activityId, operation, safePayload(payload),
                actorOrDefault(actor)));
    }

    private Mono<CompatibilityEnvelope<Map<String, Object>>> transition(Long tenantId,
                                                                        String actor,
                                                                        Long activityId,
                                                                        String transition) {
        return envelope(() -> facade.transitionActivity(tenantIdOrDefault(tenantId), activityId, transition,
                actorOrDefault(actor)));
    }

    private Mono<CompatibilityEnvelope<List<Map<String, Object>>>> list(Long tenantId,
                                                                        Long activityId,
                                                                        String resource,
                                                                        Map<String, Object> criteria,
                                                                        Integer limit) {
        return envelope(() -> facade.list(tenantIdOrDefault(tenantId), activityId, resource, criteria, limit));
    }

    private static <T> Mono<CompatibilityEnvelope<T>> envelope(ThrowingSupplier<T> supplier) {
        return Mono.fromCallable(() -> {
            try {
                return CompatibilityEnvelope.ok(supplier.get());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
            }
        });
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<CompatibilityEnvelope<Void>> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String message = exception.getReason() == null ? exception.getMessage() : exception.getReason();
        return ResponseEntity
                .status(exception.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(CompatibilityEnvelope.fail("API_001", status, message));
    }

    private static Map<String, Object> with(Map<String, Object> payload, String key, Object value) {
        Map<String, Object> result = safePayload(payload);
        result.put(key, value);
        return result;
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? DEFAULT_ACTOR : actor.trim();
    }

    public record CompatibilityEnvelope<T>(
            int code,
            String message,
            String errorCode,
            T data,
            String traceId) {

        private static <T> CompatibilityEnvelope<T> ok(T data) {
            return new CompatibilityEnvelope<>(0, "success", null, data, null);
        }

        private static <T> CompatibilityEnvelope<T> fail(String errorCode, int code, String message) {
            return new CompatibilityEnvelope<>(code, message, errorCode, null, null);
        }
    }

    private interface ThrowingSupplier<T> {
        T get();
    }
}
