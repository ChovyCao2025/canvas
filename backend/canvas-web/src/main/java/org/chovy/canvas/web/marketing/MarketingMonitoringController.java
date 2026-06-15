package org.chovy.canvas.web.marketing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MarketingMonitoringFacade;
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
@RequestMapping("/canvas/marketing-monitoring")
public class MarketingMonitoringController {

    private static final Long DEFAULT_TENANT_ID = 7L;
    private static final String DEFAULT_ACTOR = "operator-1";

    private final MarketingMonitoringFacade facade;

    public MarketingMonitoringController(MarketingMonitoringFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/sources")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertSource(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return execute(tenantId, actor, "upsertSource", payload);
    }

    @PostMapping("/items")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> ingestItem(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return execute(tenantId, actor, "ingestItem", payload);
    }

    @GetMapping("/items")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> items(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String sentimentLabel,
            @RequestParam(required = false) String competitorKey,
            @RequestParam(required = false) Integer limit) {
        return list(tenantId, "items", criteria(
                "sentimentLabel", sentimentLabel,
                "competitorKey", competitorKey), limit);
    }

    @GetMapping("/alerts")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> alerts(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return list(tenantId, "alerts", criteria("status", status), limit);
    }

    @PostMapping("/alerts/{alertId}/resolve")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> resolveAlert(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long alertId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return execute(tenantId, actor, "resolveAlert", with(payload, "alertId", alertId));
    }

    @PostMapping("/alert-channels")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertAlertChannel(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return execute(tenantId, actor, "upsertAlertChannel", payload);
    }

    @PostMapping("/alerts/{alertId}/dispatch")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> dispatchAlert(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long alertId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return execute(tenantId, actor, "dispatchAlert", with(payload, "alertId", alertId));
    }

    @GetMapping("/alert-deliveries")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> alertDeliveries(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long alertId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return list(tenantId, "alertDeliveries", criteria("alertId", alertId, "status", status), limit);
    }

    @PostMapping("/sources/{sourceId}/polling")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> configureSourcePolling(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long sourceId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return execute(tenantId, actor, "configureSourcePolling", with(payload, "sourceId", sourceId));
    }

    @PostMapping("/sources/{sourceId}/poll")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> pollSource(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long sourceId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return execute(tenantId, actor, "pollSource", with(payload, "sourceId", sourceId));
    }

    @PostMapping("/trends/snapshots/build")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> buildTrendSnapshot(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return execute(tenantId, actor, "buildTrendSnapshot", payload);
    }

    @GetMapping("/trends/snapshots")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> trendSnapshots(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String brandKey,
            @RequestParam(required = false) String competitorKey,
            @RequestParam(required = false) Integer limit) {
        return list(tenantId, "trendSnapshots", criteria(
                "sourceId", sourceId,
                "brandKey", brandKey,
                "competitorKey", competitorKey), limit);
    }

    @PostMapping("/items/{itemId}/inferences")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> analyzeInference(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long itemId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return execute(tenantId, actor, "analyzeInference", with(payload, "itemId", itemId));
    }

    @GetMapping("/inferences")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> inferences(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) String sentimentLabel,
            @RequestParam(required = false) String modelKey,
            @RequestParam(required = false) String providerStatus,
            @RequestParam(required = false) Boolean fallbackUsed,
            @RequestParam(required = false) Integer limit) {
        return list(tenantId, "inferences", criteria(
                "itemId", itemId,
                "sentimentLabel", sentimentLabel,
                "modelKey", modelKey,
                "providerStatus", providerStatus,
                "fallbackUsed", fallbackUsed), limit);
    }

    @PostMapping("/provider-credentials")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertProviderCredential(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return execute(tenantId, actor, "upsertProviderCredential", payload);
    }

    @GetMapping("/provider-credentials")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> providerCredentials(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String providerType,
            @RequestParam(required = false) String authType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return list(tenantId, "providerCredentials", criteria(
                "providerType", providerType,
                "authType", authType,
                "status", status), limit);
    }

    @PostMapping("/provider-credentials/{credentialKey}/refresh")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> refreshProviderCredential(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String credentialKey,
            @RequestBody(required = false) Map<String, Object> payload) {
        return execute(tenantId, actor, "refreshProviderCredential", with(payload, "credentialKey", credentialKey));
    }

    @PostMapping("/provider-credentials/refresh-due")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> refreshDueProviderCredentials(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody(required = false) Map<String, Object> payload) {
        return execute(tenantId, actor, "refreshDueProviderCredentials", payload);
    }

    @PostMapping("/provider-credentials/{credentialKey}/revoke")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> revokeProviderCredential(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String credentialKey,
            @RequestBody(required = false) Map<String, Object> payload) {
        return execute(tenantId, actor, "revokeProviderCredential", with(payload, "credentialKey", credentialKey));
    }

    @PostMapping("/provider-credentials/{credentialKey}/disable")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> disableProviderCredential(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable String credentialKey,
            @RequestBody(required = false) Map<String, Object> payload) {
        return execute(tenantId, actor, "disableProviderCredential", with(payload, "credentialKey", credentialKey));
    }

    @GetMapping("/provider-credentials/events")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> providerCredentialEvents(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String credentialKey,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return list(tenantId, "providerCredentialEvents", criteria(
                "credentialKey", credentialKey,
                "eventType", eventType,
                "status", status), limit);
    }

    @PostMapping("/provider-credentials/oauth/authorizations")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> startProviderOAuthAuthorization(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return execute(tenantId, actor, "startProviderOAuthAuthorization", payload);
    }

    @PostMapping("/provider-credentials/oauth/callback")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> completeProviderOAuthAuthorization(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return execute(tenantId, actor, "completeProviderOAuthAuthorization", payload);
    }

    @GetMapping("/provider-credentials/oauth/authorizations")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> providerOAuthAuthorizations(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String credentialKey,
            @RequestParam(required = false) String providerType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return list(tenantId, "providerOAuthAuthorizations", criteria(
                "credentialKey", credentialKey,
                "providerType", providerType,
                "status", status), limit);
    }

    @GetMapping("/provider-credentials/oauth/events")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> providerOAuthAuthorizationEvents(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) String authState,
            @RequestParam(required = false) String credentialKey,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return list(tenantId, "providerOAuthAuthorizationEvents", criteria(
                "authState", authState,
                "credentialKey", credentialKey,
                "eventType", eventType,
                "status", status), limit);
    }

    @PostMapping("/anomaly-rules")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> upsertAnomalyRule(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return execute(tenantId, actor, "upsertAnomalyRule", payload);
    }

    @PostMapping("/anomalies/detect")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> detectAnomalies(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @RequestBody Map<String, Object> payload) {
        return execute(tenantId, actor, "detectAnomalies", payload);
    }

    @GetMapping("/anomalies")
    public Mono<CompatibilityEnvelope<List<Map<String, Object>>>> anomalies(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(required = false) Long ruleId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return list(tenantId, "anomalies", criteria("ruleId", ruleId, "status", status), limit);
    }

    @PostMapping("/anomalies/{eventId}/resolve")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> resolveAnomaly(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long eventId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return execute(tenantId, actor, "resolveAnomaly", with(payload, "eventId", eventId));
    }

    @PostMapping("/sources/{sourceId}/webhook-secret/rotate")
    public Mono<CompatibilityEnvelope<Map<String, Object>>> rotateWebhookSecret(
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestHeader(value = "X-Actor", required = false) String actor,
            @PathVariable Long sourceId,
            @RequestBody(required = false) Map<String, Object> payload) {
        return execute(tenantId, actor, "rotateWebhookSecret", with(payload, "sourceId", sourceId));
    }

    private Mono<CompatibilityEnvelope<Map<String, Object>>> execute(Long tenantId, String actor, String operation,
                                                                     Map<String, Object> payload) {
        return envelope(() -> facade.execute(tenantIdOrDefault(tenantId), operation, safePayload(payload),
                actorOrDefault(actor)));
    }

    private Mono<CompatibilityEnvelope<List<Map<String, Object>>>> list(Long tenantId, String operation,
                                                                        Map<String, Object> criteria,
                                                                        Integer limit) {
        return envelope(() -> facade.list(tenantIdOrDefault(tenantId), operation, criteria, limit));
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

    private static Map<String, Object> criteria(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (pairs[i + 1] != null) {
                result.put(String.valueOf(pairs[i]), pairs[i + 1]);
            }
        }
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
