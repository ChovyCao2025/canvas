package org.chovy.canvas.cdp.domain;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.chovy.canvas.cdp.api.CdpTagOperationFacade.BatchTagCommand;
import org.chovy.canvas.cdp.api.CdpTagOperationFacade.TagOperationView;

public class CdpTagOperationCatalog {

    private final Clock clock;
    private final Map<Long, OperationRow> operations = new LinkedHashMap<>();
    private long operationIds;

    public CdpTagOperationCatalog(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public synchronized TagOperationView create(Long tenantId, BatchTagCommand command, String actor) {
        if (command == null) {
            throw new IllegalArgumentException("tag operation request is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        List<String> memberIds = distinct(command.memberIds());
        Map<String, Object> metadata = command.metadata() == null ? Map.of() : Map.copyOf(command.metadata());
        String status = stringValue(metadata.get("simulateStatus"), "SUCCESS").toUpperCase();
        LocalDateTime now = now();
        OperationRow row = new OperationRow(++operationIds, scopedTenantId,
                requireText(command.userId(), "userId"),
                requireText(command.tagCode(), "tagCode"),
                optional(command.tagValue()), memberIds, metadata, status, memberIds.size(), actorOrSystem(actor),
                actorOrSystem(actor), now, now);
        operations.put(row.id, row);
        return view(row);
    }

    public synchronized List<TagOperationView> listRecent(Long tenantId, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        return operations.values().stream()
                .filter(row -> Objects.equals(row.tenantId, scopedTenantId))
                .sorted(Comparator.comparing((OperationRow row) -> row.id).reversed())
                .limit(boundedLimit(limit))
                .map(CdpTagOperationCatalog::view)
                .toList();
    }

    public synchronized TagOperationView get(Long tenantId, Long id) {
        OperationRow row = requireOperation(tenantId, id);
        return view(row);
    }

    public synchronized TagOperationView retryFailed(Long tenantId, Long id, String actor) {
        OperationRow row = requireOperation(tenantId, id);
        if (!"FAILED".equals(row.status)) {
            throw new IllegalStateException("Only failed tag operations can be retried");
        }
        row.status = "RETRYING";
        row.updatedBy = actorOrSystem(actor);
        row.updatedAt = now();
        return view(row);
    }

    private OperationRow requireOperation(Long tenantId, Long id) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("id must be positive");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        OperationRow row = operations.get(id);
        if (row == null || !Objects.equals(row.tenantId, scopedTenantId)) {
            throw new IllegalArgumentException("tag operation is not found");
        }
        return row;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    private static List<String> distinct(List<String> values) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    normalized.add(value.trim());
                }
            }
        }
        return List.copyOf(normalized);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String actorOrSystem(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private static String stringValue(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private static TagOperationView view(OperationRow row) {
        return new TagOperationView(row.id, row.tenantId, row.userId, row.tagCode, row.tagValue, row.memberIds,
                row.metadata, row.status, row.affectedCount, row.createdBy, row.updatedBy, row.createdAt,
                row.updatedAt);
    }

    private static final class OperationRow {
        private final Long id;
        private final Long tenantId;
        private final String userId;
        private final String tagCode;
        private final String tagValue;
        private final List<String> memberIds;
        private final Map<String, Object> metadata;
        private String status;
        private final int affectedCount;
        private final String createdBy;
        private String updatedBy;
        private final LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private OperationRow(Long id, Long tenantId, String userId, String tagCode, String tagValue,
                List<String> memberIds, Map<String, Object> metadata, String status, int affectedCount,
                String createdBy, String updatedBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.tenantId = tenantId;
            this.userId = userId;
            this.tagCode = tagCode;
            this.tagValue = tagValue;
            this.memberIds = memberIds;
            this.metadata = metadata;
            this.status = status;
            this.affectedCount = affectedCount;
            this.createdBy = createdBy;
            this.updatedBy = updatedBy;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }
}
