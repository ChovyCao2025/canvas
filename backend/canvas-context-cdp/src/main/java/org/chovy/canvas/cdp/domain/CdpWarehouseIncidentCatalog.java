package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 维护 CdpWarehouseIncident 的内存目录和查询视图。
 */
public class CdpWarehouseIncidentCatalog {

    /**
     * DEFAULT LIMIT。
     */
    private static final int DEFAULT_LIMIT = 20;

    /**
     * MAX LIMIT。
     */
    private static final int MAX_LIMIT = 100;

    /**
     * STATUS OPEN。
     */
    private static final String STATUS_OPEN = "OPEN";

    /**
     * STATUS ACKNOWLEDGED。
     */
    private static final String STATUS_ACKNOWLEDGED = "ACKNOWLEDGED";

    /**
     * STATUS RESOLVED。
     */
    private static final String STATUS_RESOLVED = "RESOLVED";

    /**
     * DEFAULT OPERATOR。
     */
    private static final String DEFAULT_OPERATOR = "operator";

    private final Map<Long, Incident> incidents = new ConcurrentHashMap<>();

    /**
     * 创建当前组件实例。
     */
    public CdpWarehouseIncidentCatalog() {
        seed(new Incident(
                1L,
                0L,
                "QUALITY:ODS_EVENT_COUNT",
                "WAREHOUSE_QUALITY_CHECK",
                1001L,
                "WARN",
                STATUS_OPEN,
                "Warehouse quality WARN: ODS_EVENT_COUNT",
                "checkType=ODS_EVENT_COUNT, status=WARN, diff=7",
                1L,
                LocalDateTime.of(2026, 6, 5, 9, 0),
                LocalDateTime.of(2026, 6, 5, 9, 5),
                null,
                null,
                null,
                null));
        seed(new Incident(
                9001L,
                9L,
                "QUALITY:ODS_COUNT",
                "WAREHOUSE_QUALITY_CHECK",
                11L,
                "WARN",
                STATUS_OPEN,
                "Warehouse quality WARN: ODS_COUNT",
                "checkType=ODS_COUNT, status=WARN, sourceCount=98, warehouseCount=90",
                2L,
                LocalDateTime.of(2026, 6, 5, 10, 0),
                LocalDateTime.of(2026, 6, 5, 10, 10),
                null,
                null,
                null,
                null));
        seed(new Incident(
                9002L,
                9L,
                "AVAILABILITY:HYBRID:OFFLINE_AGGREGATE",
                "WAREHOUSE_AVAILABILITY",
                null,
                "CRITICAL",
                STATUS_OPEN,
                "Warehouse availability FAIL: HYBRID/OFFLINE_AGGREGATE",
                "mode=HYBRID, gateKey=OFFLINE_AGGREGATE, gateStatus=FAIL",
                1L,
                LocalDateTime.of(2026, 6, 5, 11, 0),
                LocalDateTime.of(2026, 6, 5, 11, 30),
                null,
                null,
                null,
                null));
    }

    /**
     * 查询Incidents列表。
     */
    public List<Map<String, Object>> listIncidents(Long tenantId, String status, int limit) {
        String filteredStatus = upperOrNull(status);
        return incidents.values().stream()
                .filter(incident -> incident.tenantId().equals(tenantId))
                .filter(incident -> filteredStatus == null || filteredStatus.equals(incident.status()))
                .sorted(Comparator.comparing(Incident::lastSeenAt).reversed()
                        .thenComparing(Comparator.comparing(Incident::id).reversed()))
                .limit(boundLimit(limit))
                .map(CdpWarehouseIncidentCatalog::toView)
                .toList();
    }

    /**
     * 执行 acknowledge 对应的 CDP 业务操作。
     */
    public boolean acknowledge(Long tenantId, Long incidentId, String operator) {
        requireId(incidentId);
        Incident current = incidents.get(incidentId);
        if (current == null || !tenantId.equals(current.tenantId()) || !STATUS_OPEN.equals(current.status())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        incidents.put(incidentId, current.withLifecycle(
                STATUS_ACKNOWLEDGED, now, normalizeOperator(operator), now, current.resolvedBy(), current.resolvedAt()));
        return true;
    }

    /**
     * 执行 resolve 对应的 CDP 业务操作。
     */
    public boolean resolve(Long tenantId, Long incidentId, String operator) {
        requireId(incidentId);
        Incident current = incidents.get(incidentId);
        if (current == null || !tenantId.equals(current.tenantId()) || STATUS_RESOLVED.equals(current.status())) {
            return false;
        }
        if (!List.of(STATUS_OPEN, STATUS_ACKNOWLEDGED).contains(current.status())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        incidents.put(incidentId, current.withLifecycle(
                STATUS_RESOLVED, now, current.acknowledgedBy(), current.acknowledgedAt(), normalizeOperator(operator),
                now));
        return true;
    }

    /**
     * 执行 seed 对应的 CDP 业务操作。
     */
    private void seed(Incident incident) {
        incidents.put(incident.id(), incident);
    }

    /**
     * 转换为View。
     */
    private static Map<String, Object> toView(Incident incident) {
        Map<String, Object> view = ordered();
        view.put("id", incident.id());
        view.put("tenantId", incident.tenantId());
        view.put("incidentKey", incident.incidentKey());
        view.put("sourceType", incident.sourceType());
        view.put("sourceId", incident.sourceId());
        view.put("severity", incident.severity());
        view.put("status", incident.status());
        view.put("title", incident.title());
        view.put("description", incident.description());
        view.put("occurrenceCount", incident.occurrenceCount());
        view.put("firstSeenAt", incident.firstSeenAt());
        view.put("lastSeenAt", incident.lastSeenAt());
        view.put("acknowledgedBy", incident.acknowledgedBy());
        view.put("acknowledgedAt", incident.acknowledgedAt());
        view.put("resolvedBy", incident.resolvedBy());
        view.put("resolvedAt", incident.resolvedAt());
        return view;
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
     * 执行 upperOrNull 对应的 CDP 业务操作。
     */
    private static String upperOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 归一化Operator。
     */
    private static String normalizeOperator(String operator) {
        return operator == null || operator.isBlank() ? DEFAULT_OPERATOR : operator.trim();
    }

    /**
     * 读取并校验必填的Id。
     */
    private static void requireId(Long incidentId) {
        if (incidentId == null || incidentId <= 0) {
            throw new IllegalArgumentException("incidentId must be positive");
        }
    }

    /**
     * 执行 ordered 对应的 CDP 业务操作。
     */
    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }

    /**
     * 表示 Incident 的业务数据或处理组件。
     */
    private static final class Incident {

        /**
         * 唯一标识。
         */
        private final Long id;

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * incident Key。
         */
        private final String incidentKey;

        /**
         * 来源类型。
         */
        private final String sourceType;

        /**
         * source Id。
         */
        private final Long sourceId;

        /**
         * 严重级别。
         */
        private final String severity;

        /**
         * 状态。
         */
        private final String status;

        /**
         * title。
         */
        private final String title;

        /**
         * 描述。
         */
        private final String description;

        /**
         * occurrence Count。
         */
        private final Long occurrenceCount;

        /**
         * 首次出现时间。
         */
        private final LocalDateTime firstSeenAt;

        /**
         * 最近出现时间。
         */
        private final LocalDateTime lastSeenAt;

        /**
         * acknowledged By。
         */
        private final String acknowledgedBy;

        /**
         * acknowledged At。
         */
        private final LocalDateTime acknowledgedAt;

        /**
         * resolved By。
         */
        private final String resolvedBy;

        /**
         * resolved At。
         */
        private final LocalDateTime resolvedAt;

        /**
         * 使用记录字段创建 Incident。
         */
        private Incident(
                Long id,
                Long tenantId,
                String incidentKey,
                String sourceType,
                Long sourceId,
                String severity,
                String status,
                String title,
                String description,
                Long occurrenceCount,
                LocalDateTime firstSeenAt,
                LocalDateTime lastSeenAt,
                String acknowledgedBy,
                LocalDateTime acknowledgedAt,
                String resolvedBy,
                LocalDateTime resolvedAt) {
            this.id = id;
            this.tenantId = tenantId;
            this.incidentKey = incidentKey;
            this.sourceType = sourceType;
            this.sourceId = sourceId;
            this.severity = severity;
            this.status = status;
            this.title = title;
            this.description = description;
            this.occurrenceCount = occurrenceCount;
            this.firstSeenAt = firstSeenAt;
            this.lastSeenAt = lastSeenAt;
            this.acknowledgedBy = acknowledgedBy;
            this.acknowledgedAt = acknowledgedAt;
            this.resolvedBy = resolvedBy;
            this.resolvedAt = resolvedAt;
        }

/**
 * 返回替换Lifecycle后的副本。
 */
private Incident withLifecycle(String nextStatus, LocalDateTime nextLastSeenAt, String nextAcknowledgedBy,
                                       LocalDateTime nextAcknowledgedAt, String nextResolvedBy,
                                       LocalDateTime nextResolvedAt) {
            return new Incident(id, tenantId, incidentKey, sourceType, sourceId, severity, nextStatus, title,
                    description, occurrenceCount, firstSeenAt, nextLastSeenAt, nextAcknowledgedBy, nextAcknowledgedAt,
                    nextResolvedBy, nextResolvedAt);
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
         * 返回incident Key。
         */
        public String incidentKey() {
            return incidentKey;
        }

        /**
         * 返回来源类型。
         */
        public String sourceType() {
            return sourceType;
        }

        /**
         * 返回source Id。
         */
        public Long sourceId() {
            return sourceId;
        }

        /**
         * 返回严重级别。
         */
        public String severity() {
            return severity;
        }

        /**
         * 返回状态。
         */
        public String status() {
            return status;
        }

        /**
         * 返回title。
         */
        public String title() {
            return title;
        }

        /**
         * 返回描述。
         */
        public String description() {
            return description;
        }

        /**
         * 返回occurrence Count。
         */
        public Long occurrenceCount() {
            return occurrenceCount;
        }

        /**
         * 返回首次出现时间。
         */
        public LocalDateTime firstSeenAt() {
            return firstSeenAt;
        }

        /**
         * 返回最近出现时间。
         */
        public LocalDateTime lastSeenAt() {
            return lastSeenAt;
        }

        /**
         * 返回acknowledged By。
         */
        public String acknowledgedBy() {
            return acknowledgedBy;
        }

        /**
         * 返回acknowledged At。
         */
        public LocalDateTime acknowledgedAt() {
            return acknowledgedAt;
        }

        /**
         * 返回resolved By。
         */
        public String resolvedBy() {
            return resolvedBy;
        }

        /**
         * 返回resolved At。
         */
        public LocalDateTime resolvedAt() {
            return resolvedAt;
        }

        /**
         * 按所有字段比较 Incident。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Incident that = (Incident) o;
            return java.util.Objects.equals(id, that.id)
                    && java.util.Objects.equals(tenantId, that.tenantId)
                    && java.util.Objects.equals(incidentKey, that.incidentKey)
                    && java.util.Objects.equals(sourceType, that.sourceType)
                    && java.util.Objects.equals(sourceId, that.sourceId)
                    && java.util.Objects.equals(severity, that.severity)
                    && java.util.Objects.equals(status, that.status)
                    && java.util.Objects.equals(title, that.title)
                    && java.util.Objects.equals(description, that.description)
                    && java.util.Objects.equals(occurrenceCount, that.occurrenceCount)
                    && java.util.Objects.equals(firstSeenAt, that.firstSeenAt)
                    && java.util.Objects.equals(lastSeenAt, that.lastSeenAt)
                    && java.util.Objects.equals(acknowledgedBy, that.acknowledgedBy)
                    && java.util.Objects.equals(acknowledgedAt, that.acknowledgedAt)
                    && java.util.Objects.equals(resolvedBy, that.resolvedBy)
                    && java.util.Objects.equals(resolvedAt, that.resolvedAt);
        }

        /**
         * 根据所有字段计算 Incident 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, tenantId, incidentKey, sourceType, sourceId, severity, status, title, description, occurrenceCount, firstSeenAt, lastSeenAt, acknowledgedBy, acknowledgedAt, resolvedBy, resolvedAt);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "Incident[" + "id=" + id + ", tenantId=" + tenantId + ", incidentKey=" + incidentKey + ", sourceType=" + sourceType + ", sourceId=" + sourceId + ", severity=" + severity + ", status=" + status + ", title=" + title + ", description=" + description + ", occurrenceCount=" + occurrenceCount + ", firstSeenAt=" + firstSeenAt + ", lastSeenAt=" + lastSeenAt + ", acknowledgedBy=" + acknowledgedBy + ", acknowledgedAt=" + acknowledgedAt + ", resolvedBy=" + resolvedBy + ", resolvedAt=" + resolvedAt + "]";
        }
    }
}
