package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CdpWarehouseIncidentCatalog {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_ACKNOWLEDGED = "ACKNOWLEDGED";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String DEFAULT_OPERATOR = "operator";

    private final Map<Long, Incident> incidents = new ConcurrentHashMap<>();

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

    private void seed(Incident incident) {
        incidents.put(incident.id(), incident);
    }

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

    private static int boundLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static String upperOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeOperator(String operator) {
        return operator == null || operator.isBlank() ? DEFAULT_OPERATOR : operator.trim();
    }

    private static void requireId(Long incidentId) {
        if (incidentId == null || incidentId <= 0) {
            throw new IllegalArgumentException("incidentId must be positive");
        }
    }

    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }

    private record Incident(
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

        private Incident withLifecycle(String nextStatus, LocalDateTime nextLastSeenAt, String nextAcknowledgedBy,
                                       LocalDateTime nextAcknowledgedAt, String nextResolvedBy,
                                       LocalDateTime nextResolvedAt) {
            return new Incident(id, tenantId, incidentKey, sourceType, sourceId, severity, nextStatus, title,
                    description, occurrenceCount, firstSeenAt, nextLastSeenAt, nextAcknowledgedBy, nextAcknowledgedAt,
                    nextResolvedBy, nextResolvedAt);
        }
    }
}
