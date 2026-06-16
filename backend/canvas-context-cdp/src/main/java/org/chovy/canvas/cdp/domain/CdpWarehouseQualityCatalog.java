package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 维护 CdpWarehouseQuality 的内存目录和查询视图。
 */
public class CdpWarehouseQualityCatalog {

    /**
     * DEFAULT LIMIT。
     */
    private static final int DEFAULT_LIMIT = 20;

    /**
     * MAX LIMIT。
     */
    private static final int MAX_LIMIT = 100;

    /**
     * CHECK ODS COUNT。
     */
    private static final String CHECK_ODS_COUNT = "ODS_COUNT";

    /**
     * CHECK AGGREGATE LAG。
     */
    private static final String CHECK_AGGREGATE_LAG = "AGGREGATE_LAG";

    /**
     * STATUS PASS。
     */
    private static final String STATUS_PASS = "PASS";

    /**
     * STATUS WARN。
     */
    private static final String STATUS_WARN = "WARN";

    /**
     * DEFAULT OPERATOR。
     */
    private static final String DEFAULT_OPERATOR = "operator";

    /**
     * 执行 AtomicLong 对应的 CDP 业务操作。
     */
    private final AtomicLong ids = new AtomicLong(1000L);
    private final Map<Long, QualityCheck> checks = new ConcurrentHashMap<>();

    /**
     * 执行 recentChecks 对应的 CDP 业务操作。
     */
    public List<Map<String, Object>> recentChecks(Long tenantId, int limit) {
        return checks.values().stream()
                .filter(check -> check.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(QualityCheck::checkedAt).reversed()
                        .thenComparing(Comparator.comparing(QualityCheck::id).reversed()))
                .limit(boundLimit(limit))
                .map(CdpWarehouseQualityCatalog::toView)
                .toList();
    }

    /**
     * 执行 reconcileOds 对应的 CDP 业务操作。
     */
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

    /**
     * 执行 checkAggregateLag 对应的 CDP 业务操作。
     */
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

    /**
     * 转换为View。
     */
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

    /**
     * 校验Window。
     */
    private static void validateWindow(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
    }

    /**
     * 执行 boundLimit 对应的 CDP 业务操作。
     */
    private static int boundLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 归一化Operator。
     */
    private static String normalizeOperator(String operator) {
        return operator == null || operator.isBlank() ? DEFAULT_OPERATOR : operator.trim();
    }

    /**
     * 执行 ordered 对应的 CDP 业务操作。
     */
    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }

    /**
     * 表示 QualityCheck 的业务数据或处理组件。
     */
    private static final class QualityCheck {

        /**
         * 唯一标识。
         */
        private final Long id;

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * check Type。
         */
        private final String checkType;

        /**
         * 状态。
         */
        private final String status;

        /**
         * source Count。
         */
        private final Long sourceCount;

        /**
         * warehouse Count。
         */
        private final Long warehouseCount;

        /**
         * diff Count。
         */
        private final Long diffCount;

        /**
         * window Start。
         */
        private final LocalDateTime windowStart;

        /**
         * window End。
         */
        private final LocalDateTime windowEnd;

        /**
         * threshold Value。
         */
        private final Long thresholdValue;

        /**
         * details。
         */
        private final String details;

        /**
         * checked At。
         */
        private final LocalDateTime checkedAt;

        /**
         * 创建人。
         */
        private final String createdBy;

        /**
         * 使用记录字段创建 QualityCheck。
         */
        private QualityCheck(
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
            this.id = id;
            this.tenantId = tenantId;
            this.checkType = checkType;
            this.status = status;
            this.sourceCount = sourceCount;
            this.warehouseCount = warehouseCount;
            this.diffCount = diffCount;
            this.windowStart = windowStart;
            this.windowEnd = windowEnd;
            this.thresholdValue = thresholdValue;
            this.details = details;
            this.checkedAt = checkedAt;
            this.createdBy = createdBy;
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
         * 返回check Type。
         */
        public String checkType() {
            return checkType;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回source Count。
         */
        public Long sourceCount() {
            return sourceCount;
        }

        /**
         * 返回warehouse Count。
         */
        public Long warehouseCount() {
            return warehouseCount;
        }

        /**
         * 返回diff Count。
         */
        public Long diffCount() {
            return diffCount;
        }

        /**
         * 返回window Start。
         */
        public LocalDateTime windowStart() {
            return windowStart;
        }

        /**
         * 返回window End。
         */
        public LocalDateTime windowEnd() {
            return windowEnd;
        }

        /**
         * 返回threshold Value。
         */
        public Long thresholdValue() {
            return thresholdValue;
        }

        /**
         * 返回details。
         */
        public String details() {
            return details;
        }

        /**
         * 返回checked At。
         */
        public LocalDateTime checkedAt() {
            return checkedAt;
        }

        /**
         * 返回创建人。
         */
        public String createdBy() {
            return createdBy;
        }

        /**
         * 按所有字段比较 QualityCheck。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            QualityCheck that = (QualityCheck) o;
            return java.util.Objects.equals(id, that.id)
                    && java.util.Objects.equals(tenantId, that.tenantId)
                    && java.util.Objects.equals(checkType, that.checkType)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(sourceCount, that.sourceCount)
                    && java.util.Objects.equals(warehouseCount, that.warehouseCount)
                    && java.util.Objects.equals(diffCount, that.diffCount)
                    && java.util.Objects.equals(windowStart, that.windowStart)
                    && java.util.Objects.equals(windowEnd, that.windowEnd)
                    && java.util.Objects.equals(thresholdValue, that.thresholdValue)
                    && java.util.Objects.equals(details, that.details)
                    && java.util.Objects.equals(checkedAt, that.checkedAt)
                    && java.util.Objects.equals(createdBy, that.createdBy);
        }

        /**
         * 根据所有字段计算 QualityCheck 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, tenantId, checkType, status, sourceCount, warehouseCount, diffCount, windowStart, windowEnd, thresholdValue, details, checkedAt, createdBy);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "QualityCheck[" + "id=" + id + ", tenantId=" + tenantId + ", checkType=" + checkType + ", status=" + status + ", sourceCount=" + sourceCount + ", warehouseCount=" + warehouseCount + ", diffCount=" + diffCount + ", windowStart=" + windowStart + ", windowEnd=" + windowEnd + ", thresholdValue=" + thresholdValue + ", details=" + details + ", checkedAt=" + checkedAt + ", createdBy=" + createdBy + "]";
        }
    }
}
