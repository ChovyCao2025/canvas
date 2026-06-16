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

/**
 * 维护 CdpTagOperation 的内存目录和查询视图。
 */
public class CdpTagOperationCatalog {

    /**
     * 时间源。
     */
    private final Clock clock;
    private final Map<Long, OperationRow> operations = new LinkedHashMap<>();

    /**
     * operation Ids。
     */
    private long operationIds;

    /**
     * 创建当前组件实例。
     */
    public CdpTagOperationCatalog(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 创建create。
     */
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

    /**
     * 查询Recent列表。
     */
    public synchronized List<TagOperationView> listRecent(Long tenantId, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        return operations.values().stream()
                .filter(row -> Objects.equals(row.tenantId, scopedTenantId))
                .sorted(Comparator.comparing((OperationRow row) -> row.id).reversed())
                .limit(boundedLimit(limit))
                .map(CdpTagOperationCatalog::view)
                .toList();
    }

    /**
     * 返回get。
     */
    public synchronized TagOperationView get(Long tenantId, Long id) {
        OperationRow row = requireOperation(tenantId, id);
        return view(row);
    }

    /**
     * 执行 retryFailed 对应的 CDP 业务操作。
     */
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

    /**
     * 读取并校验必填的Operation。
     */
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

    /**
     * 执行 now 对应的 CDP 业务操作。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 归一化Tenant。
     */
    private static Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 执行 boundedLimit 对应的 CDP 业务操作。
     */
    private static int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    /**
     * 执行 distinct 对应的 CDP 业务操作。
     */
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

    /**
     * 读取并校验必填的Text。
     */
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 执行 optional 对应的 CDP 业务操作。
     */
    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 执行 actorOrSystem 对应的 CDP 业务操作。
     */
    private static String actorOrSystem(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    /**
     * 执行 stringValue 对应的 CDP 业务操作。
     */
    private static String stringValue(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    /**
     * 执行 view 对应的 CDP 业务操作。
     */
    private static TagOperationView view(OperationRow row) {
        return new TagOperationView(row.id, row.tenantId, row.userId, row.tagCode, row.tagValue, row.memberIds,
                row.metadata, row.status, row.affectedCount, row.createdBy, row.updatedBy, row.createdAt,
                row.updatedAt);
    }

    /**
     * 表示 OperationRow 的业务数据或处理组件。
     */
    private static final class OperationRow {
        /**
         * 唯一标识。
         */
        private final Long id;

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * 标签编码。
         */
        private final String tagCode;

        /**
         * 标签值。
         */
        private final String tagValue;

        /**
         * member Ids。
         */
        private final List<String> memberIds;

        /**
         * metadata。
         */
        private final Map<String, Object> metadata;

        /**
         * 状态。
         */
        private String status;

        /**
         * affected Count。
         */
        private final int affectedCount;

        /**
         * 创建人。
         */
        private final String createdBy;

        /**
         * updated By。
         */
        private String updatedBy;

        /**
         * 创建时间。
         */
        private final LocalDateTime createdAt;

        /**
         * 更新时间。
         */
        private LocalDateTime updatedAt;

        /**
         * 创建当前组件实例。
         */
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
