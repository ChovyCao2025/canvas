package org.chovy.canvas.cdp.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class CdpWarehouseSloPolicyCatalog {

    public static final String DEFAULT_POLICY_KEY = "WAREHOUSE_READINESS_DEFAULT";

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final int DEFAULT_OFFLINE_WARN_RUN_GAP_MINUTES = 120;
    private static final int DEFAULT_OFFLINE_FAIL_RUN_GAP_MINUTES = 360;
    private static final int DEFAULT_OFFLINE_WARN_WATERMARK_LAG_MINUTES = 30;
    private static final int DEFAULT_OFFLINE_FAIL_WATERMARK_LAG_MINUTES = 120;
    private static final int DEFAULT_AUDIENCE_WARN_RUN_GAP_MINUTES = 1440;
    private static final int DEFAULT_AUDIENCE_FAIL_RUN_GAP_MINUTES = 4320;

    private final AtomicLong ids = new AtomicLong(1000L);
    private final Map<Long, Map<String, SloPolicy>> policiesByTenant = new ConcurrentHashMap<>();

    public List<Map<String, Object>> listPolicies(Long tenantId, String status) {
        String filteredStatus = upperOrNull(status);
        Map<String, SloPolicy> merged = new LinkedHashMap<>();
        for (Long scopedTenantId : tenantScope(tenantId)) {
            policiesByTenant.getOrDefault(scopedTenantId, Map.of()).values().stream()
                    .filter(policy -> filteredStatus == null || filteredStatus.equals(policy.status()))
                    .sorted((left, right) -> left.policyKey().compareTo(right.policyKey()))
                    .forEach(policy -> merged.put(policy.policyKey(), policy));
        }
        return new ArrayList<>(merged.values()).stream()
                .map(CdpWarehouseSloPolicyCatalog::toView)
                .toList();
    }

    public Map<String, Object> effectivePolicy(Long tenantId, String policyKey) {
        String scopedPolicyKey = policyKeyOrDefault(policyKey);
        SloPolicy selected = null;
        for (Long scopedTenantId : tenantScope(tenantId)) {
            SloPolicy candidate = policiesByTenant.getOrDefault(scopedTenantId, Map.of()).get(scopedPolicyKey);
            if (candidate != null && STATUS_ACTIVE.equals(candidate.status())) {
                selected = candidate;
            }
        }
        return toView(selected == null ? defaultPolicy(tenantId, scopedPolicyKey) : selected);
    }

    public Map<String, Object> upsertPolicy(Long tenantId, Map<String, Object> payload) {
        String policyKey = policyKeyOrDefault(stringValue(payload.get("policyKey")));
        SloPolicy policy = new SloPolicy(
                ids.incrementAndGet(),
                tenantId,
                policyKey,
                defaultString(stringValue(payload.get("displayName")), policyKey),
                positiveOrDefault(payload.get("offlineWarnRunGapMinutes"), DEFAULT_OFFLINE_WARN_RUN_GAP_MINUTES,
                        "offlineWarnRunGapMinutes"),
                positiveOrDefault(payload.get("offlineFailRunGapMinutes"), DEFAULT_OFFLINE_FAIL_RUN_GAP_MINUTES,
                        "offlineFailRunGapMinutes"),
                positiveOrDefault(payload.get("offlineWarnWatermarkLagMinutes"),
                        DEFAULT_OFFLINE_WARN_WATERMARK_LAG_MINUTES, "offlineWarnWatermarkLagMinutes"),
                positiveOrDefault(payload.get("offlineFailWatermarkLagMinutes"),
                        DEFAULT_OFFLINE_FAIL_WATERMARK_LAG_MINUTES, "offlineFailWatermarkLagMinutes"),
                positiveOrDefault(payload.get("audienceWarnRunGapMinutes"), DEFAULT_AUDIENCE_WARN_RUN_GAP_MINUTES,
                        "audienceWarnRunGapMinutes"),
                positiveOrDefault(payload.get("audienceFailRunGapMinutes"), DEFAULT_AUDIENCE_FAIL_RUN_GAP_MINUTES,
                        "audienceFailRunGapMinutes"),
                upperDefault(stringValue(payload.get("status")), STATUS_ACTIVE),
                blankToNull(stringValue(payload.get("ownerName"))),
                blankToNull(stringValue(payload.get("description"))));
        validateThresholdOrder(policy.offlineWarnRunGapMinutes(), policy.offlineFailRunGapMinutes(),
                "offline run gap");
        validateThresholdOrder(policy.offlineWarnWatermarkLagMinutes(), policy.offlineFailWatermarkLagMinutes(),
                "offline watermark lag");
        validateThresholdOrder(policy.audienceWarnRunGapMinutes(), policy.audienceFailRunGapMinutes(),
                "audience materialization run gap");
        policiesByTenant.computeIfAbsent(tenantId, ignored -> new LinkedHashMap<>()).put(policy.policyKey(), policy);
        return toView(policy);
    }

    private static SloPolicy defaultPolicy(Long tenantId, String policyKey) {
        return new SloPolicy(
                null,
                tenantId == null ? 0L : tenantId,
                policyKeyOrDefault(policyKey),
                "Warehouse Readiness Default",
                DEFAULT_OFFLINE_WARN_RUN_GAP_MINUTES,
                DEFAULT_OFFLINE_FAIL_RUN_GAP_MINUTES,
                DEFAULT_OFFLINE_WARN_WATERMARK_LAG_MINUTES,
                DEFAULT_OFFLINE_FAIL_WATERMARK_LAG_MINUTES,
                DEFAULT_AUDIENCE_WARN_RUN_GAP_MINUTES,
                DEFAULT_AUDIENCE_FAIL_RUN_GAP_MINUTES,
                STATUS_ACTIVE,
                "data-platform",
                "In-code warehouse readiness default policy.");
    }

    private static Map<String, Object> toView(SloPolicy policy) {
        Map<String, Object> view = ordered();
        view.put("id", policy.id());
        view.put("tenantId", policy.tenantId());
        view.put("policyKey", policy.policyKey());
        view.put("displayName", policy.displayName());
        view.put("offlineWarnRunGapMinutes", policy.offlineWarnRunGapMinutes());
        view.put("offlineFailRunGapMinutes", policy.offlineFailRunGapMinutes());
        view.put("offlineWarnWatermarkLagMinutes", policy.offlineWarnWatermarkLagMinutes());
        view.put("offlineFailWatermarkLagMinutes", policy.offlineFailWatermarkLagMinutes());
        view.put("audienceWarnRunGapMinutes", policy.audienceWarnRunGapMinutes());
        view.put("audienceFailRunGapMinutes", policy.audienceFailRunGapMinutes());
        view.put("status", policy.status());
        view.put("ownerName", policy.ownerName());
        view.put("description", policy.description());
        return view;
    }

    private static List<Long> tenantScope(Long tenantId) {
        if (tenantId == null || tenantId == 0L) {
            return List.of(0L);
        }
        return List.of(0L, tenantId);
    }

    private static String policyKeyOrDefault(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : DEFAULT_POLICY_KEY;
    }

    private static String upperOrNull(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private static String upperDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
    }

    private static String defaultString(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private static String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static int positiveOrDefault(Object value, int defaultValue, String fieldName) {
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        int intValue = value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
        if (intValue <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return intValue;
    }

    private static void validateThresholdOrder(int warn, int fail, String label) {
        if (warn > fail) {
            throw new IllegalArgumentException(label + " warn threshold must be <= fail threshold");
        }
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }

    private record SloPolicy(
            Long id,
            Long tenantId,
            String policyKey,
            String displayName,
            int offlineWarnRunGapMinutes,
            int offlineFailRunGapMinutes,
            int offlineWarnWatermarkLagMinutes,
            int offlineFailWatermarkLagMinutes,
            int audienceWarnRunGapMinutes,
            int audienceFailRunGapMinutes,
            String status,
            String ownerName,
            String description) {
    }
}
