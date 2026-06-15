package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class CdpWarehouseQualityCatalog {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final String CHECK_ODS_COUNT = "ODS_COUNT";
    private static final String CHECK_AGGREGATE_LAG = "AGGREGATE_LAG";
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARN = "WARN";
    private static final String DEFAULT_OPERATOR = "operator";

    private final AtomicLong ids = new AtomicLong(1000L);
    private final Map<Long, QualityCheck> checks = new ConcurrentHashMap<>();

    public List<Map<String, Object>> recentChecks(Long tenantId, int limit) {
        return checks.values().stream()
                .filter(check -> check.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(QualityCheck::checkedAt).reversed()
                        .thenComparing(Comparator.comparing(QualityCheck::id).reversed()))
                .limit(boundLimit(limit))
                .map(CdpWarehouseQualityCatalog::toView)
                .toList();
    }

    public Map<String, Object> reconcileOds(Long tenantId, LocalDateTime from, LocalDateTime to, Long tolerance,
                                            String operator) {
        validateWindow(from, to);
        long threshold = Math.max(tolerance == null ? 0L : tolerance, 0L);
        long sourceCount = 100L;
        long warehouseCount = 97L;
        long diff = Math.abs(sourceCount - warehouseCount);
        String status = diff <= threshold ? STATUS_PASS : STATUS_WARN;
        QualityCheck check = new QualityCheck(
                ids.incrementAndGet(),
                tenantId,
                CHECK_ODS_COUNT,
                status,
                sourceCount,
                warehouseCount,
                diff,
                from,
                to,
                threshold,
                "source=mysql.cdp_event_log, warehouse=canvas_ods.cdp_event_log",
                LocalDateTime.now(),
                normalizeOperator(operator));
        checks.put(check.id(), check);
        return toView(check);
    }

    public Map<String, Object> checkAggregateLag(Long tenantId, LocalDateTime now, Long maxLagMinutes,
                                                 String operator) {
        LocalDateTime effectiveNow = now == null ? LocalDateTime.now() : now;
        long threshold = Math.max(maxLagMinutes == null ? 30L : maxLagMinutes, 0L);
        long lagMinutes = 45L;
        String status = lagMinutes <= threshold ? STATUS_PASS : STATUS_WARN;
        QualityCheck check = new QualityCheck(
                ids.incrementAndGet(),
                tenantId,
                CHECK_AGGREGATE_LAG,
                status,
                null,
                null,
                lagMinutes,
                effectiveNow.minusMinutes(lagMinutes),
                effectiveNow,
                threshold,
                "watermark=" + effectiveNow.minusMinutes(lagMinutes) + ", lagMinutes=" + lagMinutes,
                LocalDateTime.now(),
                normalizeOperator(operator));
        checks.put(check.id(), check);
        return toView(check);
    }

    private static Map<String, Object> toView(QualityCheck check) {
        Map<String, Object> view = ordered();
        view.put("id", check.id());
        view.put("tenantId", check.tenantId());
        view.put("checkType", check.checkType());
        view.put("status", check.status());
        view.put("sourceCount", check.sourceCount());
        view.put("warehouseCount", check.warehouseCount());
        view.put("diffCount", check.diffCount());
        view.put("windowStart", check.windowStart());
        view.put("windowEnd", check.windowEnd());
        view.put("thresholdValue", check.thresholdValue());
        view.put("details", check.details());
        view.put("checkedAt", check.checkedAt());
        view.put("createdBy", check.createdBy());
        return view;
    }

    private static void validateWindow(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
    }

    private static int boundLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static String normalizeOperator(String operator) {
        return operator == null || operator.isBlank() ? DEFAULT_OPERATOR : operator.trim();
    }

    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }

    private record QualityCheck(
            Long id,
            Long tenantId,
            String checkType,
            String status,
            Long sourceCount,
            Long warehouseCount,
            Long diffCount,
            LocalDateTime windowStart,
            LocalDateTime windowEnd,
            Long thresholdValue,
            String details,
            LocalDateTime checkedAt,
            String createdBy) {
    }
}
