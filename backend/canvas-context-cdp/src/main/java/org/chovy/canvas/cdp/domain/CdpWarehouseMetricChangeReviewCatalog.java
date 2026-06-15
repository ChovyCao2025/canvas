package org.chovy.canvas.cdp.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.chovy.canvas.cdp.api.CdpWarehouseMetricChangeReviewFacade.MetricChangeCommand;

public class CdpWarehouseMetricChangeReviewCatalog {

    private static final String PENDING_REVIEW = "PENDING_REVIEW";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String APPLIED = "APPLIED";
    private static final Pattern SAFE_EXPRESSION = Pattern.compile("[A-Za-z0-9_\\s().,+\\-*/<>=]+");

    private final AtomicLong ids = new AtomicLong(1);
    private final List<Map<String, Object>> reviews = new ArrayList<>();
    private final Map<String, Map<String, Object>> metricContracts = new LinkedHashMap<>();
    private final Map<String, Set<String>> datasetDimensions = new LinkedHashMap<>();

    public CdpWarehouseMetricChangeReviewCatalog() {
        datasetDimensions.put("dwd_user_profile", Set.of("country", "channel"));
        metricContracts.put(key(0L, "dwd_user_profile", "profile_completeness"),
                metricSnapshot("profile_completeness", "count_if(profile_complete)", "NUMBER",
                        List.of("country")));
        metricContracts.put(key(42L, "dwd_user_profile", "profile_completeness"),
                metricSnapshot("profile_completeness", "count_if(profile_complete)", "NUMBER",
                        List.of("country")));
    }

    public synchronized List<Map<String, Object>> list(Long tenantId, String datasetKey, String metricKey,
            String status) {
        String datasetFilter = trimToNull(datasetKey);
        String metricFilter = trimToNull(metricKey);
        String statusFilter = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
        return reviews.stream()
                .filter(row -> tenantId.equals(row.get("tenantId")))
                .filter(row -> datasetFilter == null || datasetFilter.equals(row.get("datasetKey")))
                .filter(row -> metricFilter == null || metricFilter.equals(row.get("metricKey")))
                .filter(row -> statusFilter == null || statusFilter.equals(row.get("status")))
                .map(CdpWarehouseMetricChangeReviewCatalog::copy)
                .toList();
    }

    public synchronized Map<String, Object> create(Long tenantId, String requestedBy, MetricChangeCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("metric change command is required");
        }
        String datasetKey = required(command.datasetKey(), "datasetKey");
        String metricKey = required(command.metricKey(), "metricKey");
        String proposedExpression = validateExpression(command.proposedExpression());
        String reason = required(command.reason(), "reason");
        rejectOpenReview(tenantId, datasetKey, metricKey);
        Map<String, Object> currentMetric = metricContracts.get(key(tenantId, datasetKey, metricKey));
        if (currentMetric == null) {
            currentMetric = metricContracts.get(key(0L, datasetKey, metricKey));
        }
        if (currentMetric == null) {
            throw new IllegalArgumentException("Unknown metric: " + metricKey);
        }
        List<String> proposedDimensions = allowedDimensions(datasetKey, command.proposedAllowedDimensions());
        Map<String, Object> proposedMetric = metricSnapshot(metricKey, proposedExpression,
                (String) currentMetric.get("valueType"), proposedDimensions);
        Map<String, Object> row = ordered();
        row.put("id", ids.getAndIncrement());
        row.put("tenantId", tenantId);
        row.put("datasetKey", datasetKey);
        row.put("metricKey", metricKey);
        row.put("currentMetric", copy(currentMetric));
        row.put("proposedMetric", proposedMetric);
        row.put("impact", impact());
        row.put("riskLevel", "LOW");
        row.put("status", PENDING_REVIEW);
        row.put("requestedBy", requestedBy);
        row.put("requestReason", reason);
        row.put("reviewedBy", null);
        row.put("reviewedAt", null);
        row.put("reviewNote", null);
        row.put("appliedAt", null);
        row.put("createdAt", "2026-06-15T02:46:00");
        row.put("updatedAt", "2026-06-15T02:46:00");
        reviews.add(row);
        return copy(row);
    }

    public synchronized Map<String, Object> approve(Long tenantId, String reviewer, Long reviewId, String note) {
        Map<String, Object> row = review(tenantId, reviewId);
        requireStatus(row, PENDING_REVIEW, "only pending metric changes can be approved");
        row.put("status", APPROVED);
        row.put("reviewedBy", reviewer);
        row.put("reviewNote", required(note, "reviewNote"));
        row.put("reviewedAt", "2026-06-15T02:46:10");
        row.put("updatedAt", "2026-06-15T02:46:10");
        return copy(row);
    }

    public synchronized Map<String, Object> reject(Long tenantId, String reviewer, Long reviewId, String note) {
        Map<String, Object> row = review(tenantId, reviewId);
        requireStatus(row, PENDING_REVIEW, "only pending metric changes can be rejected");
        row.put("status", REJECTED);
        row.put("reviewedBy", reviewer);
        row.put("reviewNote", required(note, "reviewNote"));
        row.put("reviewedAt", "2026-06-15T02:46:10");
        row.put("updatedAt", "2026-06-15T02:46:10");
        return copy(row);
    }

    public synchronized Map<String, Object> apply(Long tenantId, Long reviewId) {
        Map<String, Object> row = review(tenantId, reviewId);
        requireStatus(row, APPROVED, "only APPROVED metric changes can be applied");
        @SuppressWarnings("unchecked")
        Map<String, Object> proposedMetric = (Map<String, Object>) row.get("proposedMetric");
        metricContracts.put(key(tenantId, (String) row.get("datasetKey"), (String) row.get("metricKey")),
                copy(proposedMetric));
        row.put("status", APPLIED);
        row.put("appliedAt", "2026-06-15T02:46:20");
        row.put("updatedAt", "2026-06-15T02:46:20");
        return copy(row);
    }

    private void rejectOpenReview(Long tenantId, String datasetKey, String metricKey) {
        boolean exists = reviews.stream()
                .anyMatch(row -> tenantId.equals(row.get("tenantId"))
                        && datasetKey.equals(row.get("datasetKey"))
                        && metricKey.equals(row.get("metricKey"))
                        && (PENDING_REVIEW.equals(row.get("status")) || APPROVED.equals(row.get("status"))));
        if (exists) {
            throw new IllegalStateException("open metric change review already exists: " + metricKey);
        }
    }

    private Map<String, Object> review(Long tenantId, Long reviewId) {
        if (reviewId == null) {
            throw new IllegalArgumentException("reviewId is required");
        }
        return reviews.stream()
                .filter(row -> tenantId.equals(row.get("tenantId")) && reviewId.equals(row.get("id")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("metric change review not found: " + reviewId));
    }

    private static void requireStatus(Map<String, Object> row, String expectedStatus, String message) {
        if (!expectedStatus.equals(row.get("status"))) {
            throw new IllegalStateException(message);
        }
    }

    private List<String> allowedDimensions(String datasetKey, List<String> dimensions) {
        Set<String> allowed = datasetDimensions.get(datasetKey);
        if (allowed == null) {
            throw new IllegalArgumentException("Unknown dataset: " + datasetKey);
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String dimension : dimensions == null ? List.<String>of() : dimensions) {
            String fieldKey = required(dimension, "allowedDimension");
            if (!allowed.contains(fieldKey)) {
                throw new IllegalArgumentException("metric allowed dimension is not a dataset field: " + fieldKey);
            }
            normalized.add(fieldKey);
        }
        return List.copyOf(normalized);
    }

    private static String validateExpression(String value) {
        String expression = required(value, "proposedExpression");
        if (!SAFE_EXPRESSION.matcher(expression).matches()
                || expression.contains("--")
                || expression.contains("/*")
                || expression.contains(";")) {
            throw new IllegalArgumentException("metric expression contains unsafe characters");
        }
        return expression;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String key(Long tenantId, String datasetKey, String metricKey) {
        return tenantId + ":" + datasetKey + ":" + metricKey;
    }

    private static Map<String, Object> metricSnapshot(
            String metricKey,
            String expression,
            String valueType,
            List<String> allowedDimensions) {
        Map<String, Object> value = ordered();
        value.put("metricKey", metricKey);
        value.put("expression", expression);
        value.put("valueType", valueType);
        value.put("allowedDimensions", allowedDimensions == null ? List.of() : List.copyOf(allowedDimensions));
        return value;
    }

    private static Map<String, Object> impact() {
        Map<String, Object> value = ordered();
        value.put("fieldDependencyCount", 0);
        value.put("lineageNodeCount", 0);
        value.put("lineageEdgeCount", 0);
        value.put("transitiveLineageNodeCount", 0);
        value.put("transitiveLineageEdgeCount", 0);
        value.put("transitivePathCount", 0);
        value.put("transitiveDownstreamNodeCount", 0);
        value.put("transitiveTruncated", false);
        value.put("chartCount", 0);
        value.put("dashboardCount", 0);
        value.put("warnings", List.of());
        return value;
    }

    private static Map<String, Object> copy(Map<String, Object> value) {
        return new LinkedHashMap<>(value);
    }

    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }
}
