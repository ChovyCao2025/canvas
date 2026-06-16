package org.chovy.canvas.cdp.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 维护 CdpWarehouseSloPolicy 的内存目录和查询视图。
 */
public class CdpWarehouseSloPolicyCatalog {

    /**
     * DEFAULT POLICY KEY。
     */
    public static final String DEFAULT_POLICY_KEY = "WAREHOUSE_READINESS_DEFAULT";

    /**
     * STATUS ACTIVE。
     */
    private static final String STATUS_ACTIVE = "ACTIVE";

    /**
     * DEFAULT OFFLINE WARN RUN GAP MINUTES。
     */
    private static final int DEFAULT_OFFLINE_WARN_RUN_GAP_MINUTES = 120;

    /**
     * DEFAULT OFFLINE FAIL RUN GAP MINUTES。
     */
    private static final int DEFAULT_OFFLINE_FAIL_RUN_GAP_MINUTES = 360;

    /**
     * DEFAULT OFFLINE WARN WATERMARK LAG MINUTES。
     */
    private static final int DEFAULT_OFFLINE_WARN_WATERMARK_LAG_MINUTES = 30;

    /**
     * DEFAULT OFFLINE FAIL WATERMARK LAG MINUTES。
     */
    private static final int DEFAULT_OFFLINE_FAIL_WATERMARK_LAG_MINUTES = 120;

    /**
     * DEFAULT AUDIENCE WARN RUN GAP MINUTES。
     */
    private static final int DEFAULT_AUDIENCE_WARN_RUN_GAP_MINUTES = 1440;

    /**
     * DEFAULT AUDIENCE FAIL RUN GAP MINUTES。
     */
    private static final int DEFAULT_AUDIENCE_FAIL_RUN_GAP_MINUTES = 4320;

    /**
     * 执行 AtomicLong 对应的 CDP 业务操作。
     */
    private final AtomicLong ids = new AtomicLong(1000L);
    private final Map<Long, Map<String, SloPolicy>> policiesByTenant = new ConcurrentHashMap<>();

    /**
     * 查询Policies列表。
     */
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

    /**
     * 执行 effectivePolicy 对应的 CDP 业务操作。
     */
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

    /**
     * 执行 upsertPolicy 对应的 CDP 业务操作。
     */
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

    /**
     * 返回默认的Policy。
     */
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

    /**
     * 转换为View。
     */
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

    /**
     * 执行 tenantScope 对应的 CDP 业务操作。
     */
    private static List<Long> tenantScope(Long tenantId) {
        if (tenantId == null || tenantId == 0L) {
            return List.of(0L);
        }
        return List.of(0L, tenantId);
    }

    /**
     * 执行 policyKeyOrDefault 对应的 CDP 业务操作。
     */
    private static String policyKeyOrDefault(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : DEFAULT_POLICY_KEY;
    }

    /**
     * 执行 upperOrNull 对应的 CDP 业务操作。
     */
    private static String upperOrNull(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    /**
     * 执行 upperDefault 对应的 CDP 业务操作。
     */
    private static String upperDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
    }

    /**
     * 返回默认的String。
     */
    private static String defaultString(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    /**
     * 执行 blankToNull 对应的 CDP 业务操作。
     */
    private static String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * 执行 positiveOrDefault 对应的 CDP 业务操作。
     */
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

    /**
     * 校验Threshold Order。
     */
    private static void validateThresholdOrder(int warn, int fail, String label) {
        if (warn > fail) {
            throw new IllegalArgumentException(label + " warn threshold must be <= fail threshold");
        }
    }

    /**
     * 执行 stringValue 对应的 CDP 业务操作。
     */
    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 执行 hasText 对应的 CDP 业务操作。
     */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 执行 ordered 对应的 CDP 业务操作。
     */
    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }

    /**
     * 表示 SloPolicy 的业务数据或处理组件。
     */
    private static final class SloPolicy {

        /**
         * 唯一标识。
         */
        private final Long id;

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * policy Key。
         */
        private final String policyKey;

        /**
         * 展示名称。
         */
        private final String displayName;

        /**
         * offline Warn Run Gap Minutes。
         */
        private final int offlineWarnRunGapMinutes;

        /**
         * offline Fail Run Gap Minutes。
         */
        private final int offlineFailRunGapMinutes;

        /**
         * offline Warn Watermark Lag Minutes。
         */
        private final int offlineWarnWatermarkLagMinutes;

        /**
         * offline Fail Watermark Lag Minutes。
         */
        private final int offlineFailWatermarkLagMinutes;

        /**
         * audience Warn Run Gap Minutes。
         */
        private final int audienceWarnRunGapMinutes;

        /**
         * audience Fail Run Gap Minutes。
         */
        private final int audienceFailRunGapMinutes;

        /**
         * 状态。
         */
        private final String status;

        /**
         * owner Name。
         */
        private final String ownerName;

        /**
         * 描述。
         */
        private final String description;

        /**
         * 使用记录字段创建 SloPolicy。
         */
        private SloPolicy(
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
            this.id = id;
            this.tenantId = tenantId;
            this.policyKey = policyKey;
            this.displayName = displayName;
            this.offlineWarnRunGapMinutes = offlineWarnRunGapMinutes;
            this.offlineFailRunGapMinutes = offlineFailRunGapMinutes;
            this.offlineWarnWatermarkLagMinutes = offlineWarnWatermarkLagMinutes;
            this.offlineFailWatermarkLagMinutes = offlineFailWatermarkLagMinutes;
            this.audienceWarnRunGapMinutes = audienceWarnRunGapMinutes;
            this.audienceFailRunGapMinutes = audienceFailRunGapMinutes;
            this.status = status;
            this.ownerName = ownerName;
            this.description = description;
        }

        /**
         * 返回唯一标识。
         */
        public Long id() {
            return id;
        }

        /**
         * 返回租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回policy Key。
         */
        public String policyKey() {
            return policyKey;
        }

        /**
         * 返回展示名称。
         */
        public String displayName() {
            return displayName;
        }

        /**
         * 返回offline Warn Run Gap Minutes。
         */
        public int offlineWarnRunGapMinutes() {
            return offlineWarnRunGapMinutes;
        }

        /**
         * 返回offline Fail Run Gap Minutes。
         */
        public int offlineFailRunGapMinutes() {
            return offlineFailRunGapMinutes;
        }

        /**
         * 返回offline Warn Watermark Lag Minutes。
         */
        public int offlineWarnWatermarkLagMinutes() {
            return offlineWarnWatermarkLagMinutes;
        }

        /**
         * 返回offline Fail Watermark Lag Minutes。
         */
        public int offlineFailWatermarkLagMinutes() {
            return offlineFailWatermarkLagMinutes;
        }

        /**
         * 返回audience Warn Run Gap Minutes。
         */
        public int audienceWarnRunGapMinutes() {
            return audienceWarnRunGapMinutes;
        }

        /**
         * 返回audience Fail Run Gap Minutes。
         */
        public int audienceFailRunGapMinutes() {
            return audienceFailRunGapMinutes;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回owner Name。
         */
        public String ownerName() {
            return ownerName;
        }

        /**
         * 返回描述。
         */
        public String description() {
            return description;
        }

        /**
         * 按所有字段比较 SloPolicy。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SloPolicy that = (SloPolicy) o;
            return java.util.Objects.equals(id, that.id)
                    && java.util.Objects.equals(tenantId, that.tenantId)
                    && java.util.Objects.equals(policyKey, that.policyKey)
                    && java.util.Objects.equals(displayName, that.displayName)
                    && java.util.Objects.equals(offlineWarnRunGapMinutes, that.offlineWarnRunGapMinutes)
                    && java.util.Objects.equals(offlineFailRunGapMinutes, that.offlineFailRunGapMinutes)
                    && java.util.Objects.equals(offlineWarnWatermarkLagMinutes, that.offlineWarnWatermarkLagMinutes)
                    && java.util.Objects.equals(offlineFailWatermarkLagMinutes, that.offlineFailWatermarkLagMinutes)
                    && java.util.Objects.equals(audienceWarnRunGapMinutes, that.audienceWarnRunGapMinutes)
                    && java.util.Objects.equals(audienceFailRunGapMinutes, that.audienceFailRunGapMinutes)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(ownerName, that.ownerName)
                    && java.util.Objects.equals(description, that.description);
        }

        /**
         * 根据所有字段计算 SloPolicy 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, tenantId, policyKey, displayName, offlineWarnRunGapMinutes, offlineFailRunGapMinutes, offlineWarnWatermarkLagMinutes, offlineFailWatermarkLagMinutes, audienceWarnRunGapMinutes, audienceFailRunGapMinutes, status, ownerName, description);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "SloPolicy[" + "id=" + id + ", tenantId=" + tenantId + ", policyKey=" + policyKey + ", displayName=" + displayName + ", offlineWarnRunGapMinutes=" + offlineWarnRunGapMinutes + ", offlineFailRunGapMinutes=" + offlineFailRunGapMinutes + ", offlineWarnWatermarkLagMinutes=" + offlineWarnWatermarkLagMinutes + ", offlineFailWatermarkLagMinutes=" + offlineFailWatermarkLagMinutes + ", audienceWarnRunGapMinutes=" + audienceWarnRunGapMinutes + ", audienceFailRunGapMinutes=" + audienceFailRunGapMinutes + ", status=" + status + ", ownerName=" + ownerName + ", description=" + description + "]";
        }
    }
}
