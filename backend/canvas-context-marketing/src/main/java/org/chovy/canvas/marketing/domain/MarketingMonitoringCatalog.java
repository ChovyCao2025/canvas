package org.chovy.canvas.marketing.domain;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 维护MarketingMonitoring相关的内存业务目录。
 */
public class MarketingMonitoringCatalog {

    /**
     * 用于生成确定性业务时间的时钟。
     */
    private final Clock clock;
    private final Map<Long, TenantState> tenants = new LinkedHashMap<>();

    /**
     * 创建MarketingMonitoringCatalog实例。
     */
    public MarketingMonitoringCatalog(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 执行execute业务操作。
     */
    public Map<String, Object> execute(Long tenantId, String operation, Map<String, Object> payload, String actor) {
        TenantState state = state(tenantId);
        Map<String, Object> safePayload = safeMap(payload);
        return switch (operation) {
            case "upsertSource" -> upsertSource(state, safePayload, actor);
            case "ingestItem" -> ingestItem(state, safePayload, actor);
            case "resolveAlert" -> transitionAlert(state, safePayload, actor, "RESOLVED");
            case "upsertAlertChannel" -> upsertStored(state.alertChannels, state.nextAlertChannelId++, tenantId,
                    operation, safePayload, actor, key(safePayload, "channelKey", "channel"));
            case "dispatchAlert" -> dispatchAlert(state, safePayload, actor);
            case "configureSourcePolling" -> upsertStored(state.pollingConfigs, state.nextPollingId++, tenantId,
                    operation, safePayload, actor, key(safePayload, "sourceId", "source"));
            case "pollSource" -> appendStored(state.pollRuns, state.nextPollRunId++, tenantId, operation, safePayload,
                    actor, "COMPLETED");
            case "buildTrendSnapshot" -> appendStored(state.trendSnapshots, state.nextTrendSnapshotId++, tenantId,
                    operation, safePayload, actor, "BUILT");
            case "analyzeInference" -> appendStored(state.inferences, state.nextInferenceId++, tenantId, operation,
                    safePayload, actor, "ANALYZED");
            case "upsertProviderCredential" -> upsertCredential(state, safePayload, actor);
            case "refreshProviderCredential" -> credentialTransition(state, safePayload, actor, "REFRESHED");
            case "refreshDueProviderCredentials" -> refreshDue(state, safePayload, actor);
            case "revokeProviderCredential" -> credentialTransition(state, safePayload, actor, "REVOKED");
            case "disableProviderCredential" -> credentialTransition(state, safePayload, actor, "DISABLED");
            case "startProviderOAuthAuthorization" -> startOAuth(state, safePayload, actor);
            case "completeProviderOAuthAuthorization" -> completeOAuth(state, safePayload, actor);
            case "upsertAnomalyRule" -> upsertStored(state.anomalyRules, state.nextAnomalyRuleId++, tenantId,
                    operation, safePayload, actor, key(safePayload, "ruleKey", "rule"));
            case "detectAnomalies" -> detectAnomalies(state, safePayload, actor);
            case "resolveAnomaly" -> transitionAnomaly(state, safePayload, actor, "RESOLVED");
            case "rotateWebhookSecret" -> rotateWebhookSecret(state, safePayload, actor);
            default -> throw new IllegalArgumentException("unsupported marketing monitoring operation: " + operation);
        };
    }

    /**
     * 查询列表。
     */
    public List<Map<String, Object>> list(Long tenantId, String operation, Map<String, Object> criteria, int limit) {
        TenantState state = state(tenantId);
        List<Map<String, Object>> rows = switch (operation) {
            case "sources" -> state.sources;
            case "items" -> state.items;
            case "alerts" -> state.alerts;
            case "alertDeliveries" -> state.alertDeliveries;
            case "trendSnapshots" -> state.trendSnapshots;
            case "inferences" -> state.inferences;
            case "providerCredentials" -> state.providerCredentials;
            case "providerCredentialEvents" -> state.providerCredentialEvents;
            case "providerOAuthAuthorizations" -> state.oauthAuthorizations;
            case "providerOAuthAuthorizationEvents" -> state.oauthEvents;
            case "anomalies" -> state.anomalyEvents;
            default -> throw new IllegalArgumentException("unsupported marketing monitoring operation: " + operation);
        };
        Map<String, Object> safeCriteria = safeMap(criteria);
        return rows.stream()
                .filter(row -> matches(row, safeCriteria))
                .sorted(Comparator.comparing(row -> Long.parseLong(String.valueOf(row.getOrDefault("id", "0")))))
                .limit(Math.max(1, Math.min(limit, 100)))
                .map(row -> {
                    Map<String, Object> copy = new LinkedHashMap<>(row);
                    return copy;
                })
                .toList();
    }

    /**
     * 执行upsertSource业务操作。
     */
    private Map<String, Object> upsertSource(TenantState state, Map<String, Object> payload, String actor) {
        return upsertStored(state.sources, state.nextSourceId++, state.tenantId, "upsertSource", payload, actor,
                key(payload, "sourceKey", "source"));
    }

    /**
     * 执行ingestItem业务操作。
     */
    private Map<String, Object> ingestItem(TenantState state, Map<String, Object> payload, String actor) {
        Map<String, Object> item = appendStored(state.items, state.nextItemId++, state.tenantId, "ingestItem", payload,
                actor, "INGESTED");
        if ("NEGATIVE".equals(item.get("sentimentLabel"))) {
            Map<String, Object> alert = appendStored(state.alerts, state.nextAlertId++, state.tenantId, "alert",
                    Map.of("itemId", item.get("id"), "status", "OPEN"), actor, "OPEN");
            item.put("alert", alert);
        }
        return item;
    }

    /**
     * 执行dispatchAlert业务操作。
     */
    private Map<String, Object> dispatchAlert(TenantState state, Map<String, Object> payload, String actor) {
        Map<String, Object> dispatch = appendStored(state.alertDispatches, state.nextDispatchId++, state.tenantId,
                "dispatchAlert", payload, actor, "DISPATCHED");
        dispatch.put("channelCount", state.alertChannels.size());
        Map<String, Object> delivery = appendStored(state.alertDeliveries, state.nextDeliveryId++, state.tenantId,
                "alertDelivery", Map.of("alertId", value(payload, "alertId"), "status", "SENT"), actor, "SENT");
        dispatch.put("deliveries", List.of(delivery));
        return dispatch;
    }

    /**
     * 执行upsertCredential业务操作。
     */
    private Map<String, Object> upsertCredential(TenantState state, Map<String, Object> payload, String actor) {
        Map<String, Object> credential = upsertStored(state.providerCredentials, state.nextCredentialId++, state.tenantId,
                "upsertProviderCredential", payload, actor, key(payload, "credentialKey", "credential"));
        credential.put("status", "ACTIVE");
        appendCredentialEvent(state, credential.get("credentialKey"), "UPSERTED", "OK", actor);
        return credential;
    }

    /**
     * 执行credentialTransition业务操作。
     */
    private Map<String, Object> credentialTransition(TenantState state, Map<String, Object> payload, String actor,
                                                     String status) {
        String credentialKey = key(payload, "credentialKey", "credential");
        Map<String, Object> credential = state.providerCredentials.stream()
                .filter(row -> credentialKey.equals(row.get("credentialKey")))
                .findFirst()
                .orElseGet(() -> upsertStored(state.providerCredentials, state.nextCredentialId++, state.tenantId,
                        "upsertProviderCredential", payload, actor, credentialKey));
        credential.put("status", status);
        credential.put("updatedBy", actor);
        appendCredentialEvent(state, credentialKey, status, "OK", actor);
        return new LinkedHashMap<>(credential);
    }

    /**
     * 执行refreshDue业务操作。
     */
    private Map<String, Object> refreshDue(TenantState state, Map<String, Object> payload, String actor) {
        int limit = toInt(payload.get("limit"), 50);
        List<Map<String, Object>> refreshed = state.providerCredentials.stream()
                .limit(Math.max(1, Math.min(limit, 100)))
                .map(row -> credentialTransition(state, Map.of("credentialKey", row.get("credentialKey")), actor,
                        "REFRESHED"))
                .toList();
        Map<String, Object> result = base(state.tenantId, "refreshDueProviderCredentials", payload, actor, "REFRESHED");
        result.put("refreshedCount", refreshed.size());
        result.put("credentials", refreshed);
        return result;
    }

    /**
     * 执行startOAuth业务操作。
     */
    private Map<String, Object> startOAuth(TenantState state, Map<String, Object> payload, String actor) {
        Map<String, Object> auth = appendStored(state.oauthAuthorizations, state.nextOAuthId++, state.tenantId,
                "startProviderOAuthAuthorization", payload, actor, "STARTED");
        String authState = key(payload, "state", "oauth-" + auth.get("id"));
        auth.put("authState", authState);
        auth.put("authorizationUrl", "https://provider.example/oauth/authorize?state=" + authState);
        appendOAuthEvent(state, authState, value(payload, "credentialKey"), "STARTED", "OK", actor);
        return auth;
    }

    /**
     * 执行completeOAuth业务操作。
     */
    private Map<String, Object> completeOAuth(TenantState state, Map<String, Object> payload, String actor) {
        Map<String, Object> auth = appendStored(state.oauthAuthorizations, state.nextOAuthId++, state.tenantId,
                "completeProviderOAuthAuthorization", payload, actor, "COMPLETED");
        String authState = key(payload, "state", "oauth-" + auth.get("id"));
        auth.put("authState", authState);
        appendOAuthEvent(state, authState, value(payload, "credentialKey"), "COMPLETED", "OK", actor);
        return auth;
    }

    /**
     * 执行detectAnomalies业务操作。
     */
    private Map<String, Object> detectAnomalies(TenantState state, Map<String, Object> payload, String actor) {
        Map<String, Object> event = appendStored(state.anomalyEvents, state.nextAnomalyEventId++, state.tenantId,
                "anomaly", payload, actor, "OPEN");
        Map<String, Object> detection = base(state.tenantId, "detectAnomalies", payload, actor, "DETECTED");
        detection.put("events", List.of(event));
        return detection;
    }

    /**
     * 执行transitionAlert业务操作。
     */
    private Map<String, Object> transitionAlert(TenantState state, Map<String, Object> payload, String actor,
                                                String status) {
        return transition(state.alerts, state.tenantId, "resolveAlert", "alertId", payload, actor, status);
    }

    /**
     * 执行transitionAnomaly业务操作。
     */
    private Map<String, Object> transitionAnomaly(TenantState state, Map<String, Object> payload, String actor,
                                                  String status) {
        return transition(state.anomalyEvents, state.tenantId, "resolveAnomaly", "eventId", payload, actor, status);
    }

    /**
     * 执行rotateWebhookSecret业务操作。
     */
    private Map<String, Object> rotateWebhookSecret(TenantState state, Map<String, Object> payload, String actor) {
        Map<String, Object> secret = base(state.tenantId, "rotateWebhookSecret", payload, actor, "ROTATED");
        secret.put("sourceId", value(payload, "sourceId"));
        secret.put("secretVersion", "v" + state.nextSecretVersion++);
        return secret;
    }

    /**
     * 执行upsertStored业务操作。
     */
    private Map<String, Object> upsertStored(List<Map<String, Object>> rows, long id, Long tenantId, String operation,
                                             Map<String, Object> payload, String actor, String businessKey) {
        Map<String, Object> existing = rows.stream()
                .filter(row -> businessKey.equals(row.get("businessKey")))
                .findFirst()
                .orElse(null);
        Map<String, Object> row = existing == null
                ? base(tenantId, operation, payload, actor, "ACTIVE")
                : existing;
        if (existing == null) {
            row.put("id", id);
            row.put("businessKey", businessKey);
            row.put("createdBy", actor);
            rows.add(row);
        }
        row.putAll(payload);
        row.put("updatedBy", actor);
        normalizeCommonFields(row);
        return new LinkedHashMap<>(row);
    }

    /**
     * 执行appendStored业务操作。
     */
    private Map<String, Object> appendStored(List<Map<String, Object>> rows, long id, Long tenantId, String operation,
                                             Map<String, Object> payload, String actor, String status) {
        Map<String, Object> row = base(tenantId, operation, payload, actor, status);
        row.put("id", id);
        row.putAll(payload);
        row.put("createdBy", actor);
        row.put("updatedBy", actor);
        normalizeCommonFields(row);
        rows.add(row);
        return new LinkedHashMap<>(row);
    }

    /**
     * 执行transition业务操作。
     */
    private Map<String, Object> transition(List<Map<String, Object>> rows, Long tenantId, String operation,
                                           String idField, Map<String, Object> payload, String actor, String status) {
        Object id = value(payload, idField);
        Map<String, Object> row = rows.stream()
                .filter(existing -> Objects.equals(String.valueOf(existing.get("id")), String.valueOf(id)))
                .findFirst()
                .orElseGet(() -> appendStored(rows, toLong(id, rows.size() + 1L), tenantId, operation, payload, actor,
                        "OPEN"));
        row.put("status", status);
        row.put("updatedBy", actor);
        return new LinkedHashMap<>(row);
    }

    /**
     * 执行appendCredentialEvent业务操作。
     */
    private void appendCredentialEvent(TenantState state, Object credentialKey, String eventType, String status,
                                       String actor) {
        appendStored(state.providerCredentialEvents, state.nextCredentialEventId++, state.tenantId,
                "providerCredentialEvent", Map.of(
                        "credentialKey", credentialKey == null ? "" : credentialKey,
                        "eventType", eventType,
                        "status", status), actor, status);
    }

    /**
     * 执行appendOAuthEvent业务操作。
     */
    private void appendOAuthEvent(TenantState state, Object authState, Object credentialKey, String eventType,
                                  String status, String actor) {
        appendStored(state.oauthEvents, state.nextOAuthEventId++, state.tenantId,
                "providerOAuthAuthorizationEvent", Map.of(
                        "authState", authState == null ? "" : authState,
                        "credentialKey", credentialKey == null ? "" : credentialKey,
                        "eventType", eventType,
                        "status", status), actor, status);
    }

    /**
     * 执行base业务操作。
     */
    private Map<String, Object> base(Long tenantId, String operation, Map<String, Object> payload, String actor,
                                     String status) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("tenantId", tenantId);
        row.put("operation", operation);
        row.put("status", status);
        row.put("payload", new LinkedHashMap<>(payload));
        row.put("occurredAt", clock.instant().toString());
        row.put("updatedBy", actor);
        return row;
    }

    /**
     * 执行matches业务操作。
     */
    private boolean matches(Map<String, Object> row, Map<String, Object> criteria) {
        for (Map.Entry<String, Object> entry : criteria.entrySet()) {
            if (entry.getValue() == null || String.valueOf(entry.getValue()).isBlank() || "limit".equals(entry.getKey())) {
                continue;
            }
            Object rowValue = row.get(entry.getKey());
            if (rowValue != null && !String.valueOf(rowValue).equalsIgnoreCase(String.valueOf(entry.getValue()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 执行state业务操作。
     */
    private TenantState state(Long tenantId) {
        return tenants.computeIfAbsent(tenantId, TenantState::new);
    }

    /**
     * 执行safeMap业务操作。
     */
    private Map<String, Object> safeMap(Map<String, Object> payload) {
        return payload == null ? Map.of() : new LinkedHashMap<>(payload);
    }

    /**
     * 规范化commonFields输入值。
     */
    private void normalizeCommonFields(Map<String, Object> row) {
        uppercase(row, "sentimentLabel");
        uppercase(row, "providerType");
        uppercase(row, "authType");
        copyBusinessKey(row, "sourceKey");
        copyBusinessKey(row, "channelKey");
        copyBusinessKey(row, "credentialKey");
        copyBusinessKey(row, "ruleKey");
    }

    /**
     * 执行uppercase业务操作。
     */
    private void uppercase(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof String text && !text.isBlank()) {
            row.put(key, text.trim().toUpperCase());
        }
    }

    /**
     * 执行copyBusinessKey业务操作。
     */
    private void copyBusinessKey(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value != null) {
            row.put(key, String.valueOf(value).trim());
        }
    }

    /**
     * 执行key业务操作。
     */
    private static String key(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }

    /**
     * 执行value业务操作。
     */
    private static Object value(Map<String, Object> payload, String key) {
        return payload.get(key);
    }

    /**
     * 转换为long对象。
     */
    private static long toLong(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? fallback : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    /**
     * 转换为int对象。
     */
    private static int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    /**
     * 提供TenantState的业务能力。
     */
    private static final class TenantState {
        /**
         * 保存tenantId字段值。
         */
        private final Long tenantId;
        private final List<Map<String, Object>> sources = new ArrayList<>();
        private final List<Map<String, Object>> items = new ArrayList<>();
        private final List<Map<String, Object>> alerts = new ArrayList<>();
        private final List<Map<String, Object>> alertChannels = new ArrayList<>();
        private final List<Map<String, Object>> alertDispatches = new ArrayList<>();
        private final List<Map<String, Object>> alertDeliveries = new ArrayList<>();
        private final List<Map<String, Object>> pollingConfigs = new ArrayList<>();
        private final List<Map<String, Object>> pollRuns = new ArrayList<>();
        private final List<Map<String, Object>> trendSnapshots = new ArrayList<>();
        private final List<Map<String, Object>> inferences = new ArrayList<>();
        private final List<Map<String, Object>> providerCredentials = new ArrayList<>();
        private final List<Map<String, Object>> providerCredentialEvents = new ArrayList<>();
        private final List<Map<String, Object>> oauthAuthorizations = new ArrayList<>();
        private final List<Map<String, Object>> oauthEvents = new ArrayList<>();
        private final List<Map<String, Object>> anomalyRules = new ArrayList<>();
        private final List<Map<String, Object>> anomalyEvents = new ArrayList<>();

        /**
         * 保存nextSourceId字段值。
         */
        private long nextSourceId = 10L;

        /**
         * 保存nextItemId字段值。
         */
        private long nextItemId = 100L;

        /**
         * 保存nextAlertId字段值。
         */
        private long nextAlertId = 200L;

        /**
         * 保存nextAlertChannelId字段值。
         */
        private long nextAlertChannelId = 300L;

        /**
         * 保存nextDispatchId字段值。
         */
        private long nextDispatchId = 400L;

        /**
         * 保存nextDeliveryId字段值。
         */
        private long nextDeliveryId = 500L;

        /**
         * 保存nextPollingId字段值。
         */
        private long nextPollingId = 600L;

        /**
         * 保存nextPollRunId字段值。
         */
        private long nextPollRunId = 700L;

        /**
         * 保存nextTrendSnapshotId字段值。
         */
        private long nextTrendSnapshotId = 800L;

        /**
         * 保存nextInferenceId字段值。
         */
        private long nextInferenceId = 900L;

        /**
         * 保存nextCredentialId字段值。
         */
        private long nextCredentialId = 1000L;

        /**
         * 保存nextCredentialEventId字段值。
         */
        private long nextCredentialEventId = 1100L;

        /**
         * 保存nextOAuthId字段值。
         */
        private long nextOAuthId = 1200L;

        /**
         * 保存nextOAuthEventId字段值。
         */
        private long nextOAuthEventId = 1300L;

        /**
         * 保存nextAnomalyRuleId字段值。
         */
        private long nextAnomalyRuleId = 1400L;

        /**
         * 保存nextAnomalyEventId字段值。
         */
        private long nextAnomalyEventId = 1500L;

        /**
         * 保存nextSecretVersion字段值。
         */
        private long nextSecretVersion = 1L;

        /**
         * 创建TenantState实例。
         */
        private TenantState(Long tenantId) {
            this.tenantId = tenantId;
        }
    }
}
